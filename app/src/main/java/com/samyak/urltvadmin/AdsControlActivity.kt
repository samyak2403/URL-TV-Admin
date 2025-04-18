package com.samyak.urltvadmin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.samyak.urltvadmin.databinding.ActivityAdsControlBinding
import android.text.Editable
import android.text.TextWatcher
import android.os.Handler
import android.os.Looper

class AdsControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdsControlBinding
    private var hasChanges = false
    
    // Firebase reference for storing ad configuration
    private val database = FirebaseDatabase.getInstance()
    private val adConfigRef = database.getReference("admob") // Direct reference to admob node

    // AdmobAds data class to match your AdManage implementation
    data class AdmobAds(
        var bannerAd: String? = null,
        var interstitialAd: String? = null,
        var appOpenAd: String? = null,
        var interstitialAdDelay: Int = 3,
        // int ad controllers
        var interstitialAdRetriesThreshold: Int = 10,
        var showInterstitialAdInstantly: Boolean = false,
        var showProgressInterstitialAdInstantly: Boolean = true,
        var permenantHideInterstitialAdProgress: Boolean = false,
        var loadInterstitialAdAgainAfterShowing: Boolean = true,
        var dialogMessage: String = "Please Wait Showing Ad...",
        var dialogCancellable: Boolean = false,
        // banner ad controllers
        var bannerAdRetriesThreshold: Int = 10,
        var bannerAdShowWaterMark: Boolean = true,
        var bannerAdWaterMarkText: String = "<br>Loading Banner Ad<br>",
        var bannerAdAnimation: Boolean = true,
        var bannerAdHideWaterMarkOnError: Boolean = true,
        var bannerAdAnimationDuration: Long = 500L,
        var bannerAdWaterMarkBG: String? = null,
        var bannerAdWaterMarkFG: String? = null,
        // Native ad settings
        var nativeAd: String? = null,
        var nativeAdShowShimmer: Boolean = true,
        var nativeAdAnimationDuration: Long = 500L,
        // Ad enabled flags
        var bannerAdEnabled: Boolean = true,
        var interstitialAdEnabled: Boolean = true,
        var appOpenAdEnabled: Boolean = true,
        var nativeAdEnabled: Boolean = true,
        var rewardedAdEnabled: Boolean = false
    )
    
    private var admobAds = AdmobAds()

    // Add a handler for delayed operations
    private val handler = Handler(Looper.getMainLooper())
    
    // Add a flag to prevent multiple simultaneous saves
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityAdsControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ads Control"

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)

        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.alpha = 0.5f
        
        // Improve error handling for Firebase initialization
        try {
            // Load ad configuration from Firebase
            loadAdConfiguration()
        } catch (e: Exception) {
            Log.e("AdsControlActivity", "Error initializing Firebase", e)
            Toast.makeText(this, "Error connecting to database: ${e.message}", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
            binding.contentLayout.alpha = 1.0f
            setupUI() // Still setup UI even if Firebase fails
        }
        
        // Setup save button
        binding.saveSettingsButton.setOnClickListener {
            saveAdConfiguration()
        }
        
        // Setup bulk edit button
        binding.bulkEditButton.setOnClickListener {
            showBulkEditDialog()
        }
        
        // Setup apply to all button
        binding.applyToAllPlatformsButton.setOnClickListener {
            showApplyToAllDialog()
        }
        
        // Setup test ads button
        binding.useTestAdsButton.setOnClickListener {
            applyTestAdIds()
        }
    }
    
    private fun setupUI() {
        // Setup ad type switches with immediate updates to Firebase
        binding.bannerAdsSwitch.setOnCheckedChangeListener { _, isChecked ->
            try {
                // Update UI elements based on switch state
                binding.bannerAdIdEditText.isEnabled = isChecked
                updateAdFieldsVisibility("banner", isChecked)
                
                // Only update Firebase if this is a user action (not during initial setup)
                if (binding.root.isAttachedToWindow) {
                    updateAdEnabledState("banner", isChecked)
                }
                
                hasChanges = true
                
                // Ensure watermark fields are properly updated
                if (!isChecked) {
                    // If banner ads are disabled, also disable watermark fields
                    binding.watermarkSwitch.isEnabled = false
                    updateWatermarkFieldsVisibility(false)
                } else {
                    // If banner ads are enabled, enable watermark switch
                    binding.watermarkSwitch.isEnabled = true
                    // And update watermark fields based on watermark switch state
                    updateWatermarkFieldsVisibility(binding.watermarkSwitch.isChecked)
                }
            } catch (e: Exception) {
                Log.e("AdsControlActivity", "Error in banner switch listener", e)
                // Revert switch state if there's an error
                binding.bannerAdsSwitch.isChecked = !isChecked
                Toast.makeText(this, "Error updating banner ad state: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.interstitialAdsSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.interstitialAdIdEditText.isEnabled = isChecked
            updateAdFieldsVisibility("interstitial", isChecked)
            updateAdEnabledState("interstitial", isChecked)
            hasChanges = true
        }
        
        binding.appOpenAdsSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.appOpenAdIdEditText.isEnabled = isChecked
            updateAdEnabledState("appOpen", isChecked)
            hasChanges = true
        }
        
        binding.nativeAdsSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.nativeAdIdEditText.isEnabled = isChecked
            updateAdFieldsVisibility("native", isChecked)
            updateAdEnabledState("native", isChecked)
            hasChanges = true
        }
        
        binding.rewardedAdsSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateAdEnabledState("rewarded", isChecked)
            hasChanges = true
        }
        
        // Setup watermark switch
        binding.watermarkSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateWatermarkFieldsVisibility(isChecked)
            hasChanges = true
        }
        
        // Setup text change listeners for all EditText fields
        setupTextChangeListeners()
    }
    
    private fun setupTextChangeListeners() {
        val textWatchers = listOf(
            binding.bannerAdIdEditText,
            binding.interstitialAdIdEditText,
            binding.appOpenAdIdEditText,
            binding.nativeAdIdEditText,
            binding.watermarkTextEditText,
            binding.watermarkBgColorEditText,
            binding.watermarkFgColorEditText,
            binding.animationDurationEditText,
            binding.interstitialDelayEditText,
            binding.dialogMessageEditText,
            binding.nativeAdAnimationDurationEditText,
            binding.bannerRetriesThresholdEditText,
            binding.interstitialRetriesThresholdEditText
        )
        
        textWatchers.forEach { editText ->
            // Replace focus listener with TextWatcher for better change detection
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    hasChanges = true
                }
                
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        
        // Setup other switches
        val switches = listOf(
            binding.bannerAnimationSwitch,
            binding.hideWatermarkOnErrorSwitch,
            binding.showInterstitialInstantlySwitch,
            binding.showProgressInterstitialInstantlySwitch,
            binding.hideInterstitialProgressSwitch,
            binding.loadInterstitialAgainSwitch,
            binding.dialogCancellableSwitch,
            binding.nativeAdShimmerSwitch
        )
        
        switches.forEach { switch ->
            switch.setOnCheckedChangeListener { _, _ ->
                hasChanges = true
            }
        }
    }
    
    // New method to update the enabled state in Firebase immediately
    private fun updateAdEnabledState(adType: String, isEnabled: Boolean) {
        try {
            val updates = HashMap<String, Any>()
            
            when (adType) {
                "banner" -> {
                    updates["bannerAdEnabled"] = isEnabled
                    // If disabling, we don't clear the ID, just set enabled to false
                    // This allows users to keep their ad IDs for future use
                    
                    // Update local model to match
                    admobAds.bannerAdEnabled = isEnabled
                }
                "interstitial" -> {
                    updates["interstitialAdEnabled"] = isEnabled
                    admobAds.interstitialAdEnabled = isEnabled
                }
                "appOpen" -> {
                    updates["appOpenAdEnabled"] = isEnabled
                    admobAds.appOpenAdEnabled = isEnabled
                }
                "native" -> {
                    updates["nativeAdEnabled"] = isEnabled
                    admobAds.nativeAdEnabled = isEnabled
                }
                "rewarded" -> {
                    updates["rewardedAdEnabled"] = isEnabled
                    admobAds.rewardedAdEnabled = isEnabled
                }
            }
            
            // Update only the enabled flag in Firebase
            if (updates.isNotEmpty()) {
                adConfigRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("AdsControlActivity", "$adType ads ${if (isEnabled) "enabled" else "disabled"}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("AdsControlActivity", "Error updating $adType enabled state: ${e.message}")
                        Toast.makeText(this, "Failed to update $adType state", Toast.LENGTH_SHORT).show()
                        
                        // Revert local model if Firebase update fails
                        when (adType) {
                            "banner" -> admobAds.bannerAdEnabled = !isEnabled
                            "interstitial" -> admobAds.interstitialAdEnabled = !isEnabled
                            "appOpen" -> admobAds.appOpenAdEnabled = !isEnabled
                            "native" -> admobAds.nativeAdEnabled = !isEnabled
                            "rewarded" -> admobAds.rewardedAdEnabled = !isEnabled
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("AdsControlActivity", "Error in updateAdEnabledState", e)
            Toast.makeText(this, "Error updating ad state: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateAdFieldsVisibility(adType: String, isVisible: Boolean) {
        when (adType) {
            "banner" -> {
                binding.bannerAnimationSwitch.isEnabled = isVisible
                binding.animationDurationEditText.isEnabled = isVisible
                binding.watermarkSwitch.isEnabled = isVisible
                binding.hideWatermarkOnErrorSwitch.isEnabled = isVisible
                binding.bannerRetriesThresholdEditText.isEnabled = isVisible
                
                // Also update watermark fields
                val watermarkEnabled = isVisible && binding.watermarkSwitch.isChecked
                updateWatermarkFieldsVisibility(watermarkEnabled)
            }
            "interstitial" -> {
                binding.interstitialDelayEditText.isEnabled = isVisible
                binding.interstitialRetriesThresholdEditText.isEnabled = isVisible
                binding.showInterstitialInstantlySwitch.isEnabled = isVisible
                binding.showProgressInterstitialInstantlySwitch.isEnabled = isVisible
                binding.hideInterstitialProgressSwitch.isEnabled = isVisible
                binding.loadInterstitialAgainSwitch.isEnabled = isVisible
                binding.dialogMessageEditText.isEnabled = isVisible
                binding.dialogCancellableSwitch.isEnabled = isVisible
            }
            "native" -> {
                binding.nativeAdShimmerSwitch.isEnabled = isVisible
                binding.nativeAdAnimationDurationEditText.isEnabled = isVisible
            }
        }
    }
    
    private fun updateWatermarkFieldsVisibility(isVisible: Boolean) {
        binding.watermarkTextEditText.isEnabled = isVisible
        binding.watermarkBgColorEditText.isEnabled = isVisible
        binding.watermarkFgColorEditText.isEnabled = isVisible
    }
    
    private fun loadAdConfiguration() {
        adConfigRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.alpha = 1.0f
                    
                    if (!snapshot.exists()) {
                        Log.w("AdsControlActivity", "No ad configuration found. Using defaults.")
                        Toast.makeText(this@AdsControlActivity, "No ad configuration found. Using defaults.", Toast.LENGTH_SHORT).show()
                        
                        // Setup UI after loading defaults
                        setupUI()
                        return
                    }
                    
                    // Convert snapshot to AdmobAds object
                    val loadedAdmobAds = snapshot.getValue(AdmobAds::class.java)
                    if (loadedAdmobAds != null) {
                        admobAds = loadedAdmobAds
                        
                        // Update UI with loaded values
                        updateUIFromConfig()
                        
                        // Setup UI after loading configuration
                        setupUI()
                        
                        Log.d("AdsControlActivity", "Ad configuration loaded successfully")
                    } else {
                        Log.w("AdsControlActivity", "Failed to parse AdmobAds data, using defaults")
                        Toast.makeText(this@AdsControlActivity, "Failed to parse ad configuration. Using defaults.", Toast.LENGTH_SHORT).show()
                        setupUI()
                    }
                } catch (e: Exception) {
                    Log.e("AdsControlActivity", "Error loading ad configuration", e)
                    Toast.makeText(this@AdsControlActivity, "Error loading ad configuration: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // Setup UI even if there's an error
                    setupUI()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Hide loading indicator
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.alpha = 1.0f
                
                Log.e("AdsControlActivity", "Database error: ${error.message}")
                Toast.makeText(this@AdsControlActivity, "Failed to load ad configuration: ${error.message}", Toast.LENGTH_SHORT).show()
                
                // Setup UI even if there's an error
                setupUI()
            }
        })
    }
    
    private fun updateUIFromConfig() {
        try {
            // Ad IDs
            binding.bannerAdIdEditText.setText(admobAds.bannerAd ?: "")
            binding.interstitialAdIdEditText.setText(admobAds.interstitialAd ?: "")
            binding.appOpenAdIdEditText.setText(admobAds.appOpenAd ?: "")
            binding.nativeAdIdEditText.setText(admobAds.nativeAd ?: "")
            
            // Ad switches - use the enabled flags directly
            binding.bannerAdsSwitch.isChecked = admobAds.bannerAdEnabled
            binding.interstitialAdsSwitch.isChecked = admobAds.interstitialAdEnabled
            binding.appOpenAdsSwitch.isChecked = admobAds.appOpenAdEnabled
            binding.nativeAdsSwitch.isChecked = admobAds.nativeAdEnabled
            binding.rewardedAdsSwitch.isChecked = admobAds.rewardedAdEnabled
            
            // Banner settings
            binding.bannerAnimationSwitch.isChecked = admobAds.bannerAdAnimation
            binding.watermarkSwitch.isChecked = admobAds.bannerAdShowWaterMark
            binding.watermarkTextEditText.setText(admobAds.bannerAdWaterMarkText)
            binding.watermarkBgColorEditText.setText(admobAds.bannerAdWaterMarkBG ?: "#F8C9D0")
            binding.watermarkFgColorEditText.setText(admobAds.bannerAdWaterMarkFG ?: "#BA4F60")
            binding.animationDurationEditText.setText(admobAds.bannerAdAnimationDuration.toString())
            binding.hideWatermarkOnErrorSwitch.isChecked = admobAds.bannerAdHideWaterMarkOnError
            binding.bannerRetriesThresholdEditText.setText(admobAds.bannerAdRetriesThreshold.toString())
            
            // Interstitial settings
            binding.interstitialDelayEditText.setText(admobAds.interstitialAdDelay.toString())
            binding.interstitialRetriesThresholdEditText.setText(admobAds.interstitialAdRetriesThreshold.toString())
            binding.showInterstitialInstantlySwitch.isChecked = admobAds.showInterstitialAdInstantly
            binding.showProgressInterstitialInstantlySwitch.isChecked = admobAds.showProgressInterstitialAdInstantly
            binding.hideInterstitialProgressSwitch.isChecked = admobAds.permenantHideInterstitialAdProgress
            binding.loadInterstitialAgainSwitch.isChecked = admobAds.loadInterstitialAdAgainAfterShowing
            
            // Dialog settings
            binding.dialogMessageEditText.setText(admobAds.dialogMessage)
            binding.dialogCancellableSwitch.isChecked = admobAds.dialogCancellable
            
            // Native ad settings
            binding.nativeAdShimmerSwitch.isChecked = admobAds.nativeAdShowShimmer
            binding.nativeAdAnimationDurationEditText.setText(admobAds.nativeAdAnimationDuration.toString())
            
            // Update field visibility based on switch states
            updateAdFieldsVisibility("banner", binding.bannerAdsSwitch.isChecked)
            updateAdFieldsVisibility("interstitial", binding.interstitialAdsSwitch.isChecked)
            updateAdFieldsVisibility("native", binding.nativeAdsSwitch.isChecked)
            updateWatermarkFieldsVisibility(binding.watermarkSwitch.isChecked && binding.bannerAdsSwitch.isChecked)
            
            // Update enabled state of edit texts
            binding.bannerAdIdEditText.isEnabled = binding.bannerAdsSwitch.isChecked
            binding.interstitialAdIdEditText.isEnabled = binding.interstitialAdsSwitch.isChecked
            binding.appOpenAdIdEditText.isEnabled = binding.appOpenAdsSwitch.isChecked
            binding.nativeAdIdEditText.isEnabled = binding.nativeAdsSwitch.isChecked
            
            // Update watermark switch enabled state based on banner switch
            binding.watermarkSwitch.isEnabled = binding.bannerAdsSwitch.isChecked
        } catch (e: Exception) {
            Log.e("AdsControlActivity", "Error in updateUIFromConfig", e)
            Toast.makeText(this, "Error updating UI: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveAdConfiguration() {
        // Prevent multiple simultaneous saves
        if (isSaving) {
            Toast.makeText(this, "Save in progress, please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Show loading indicator
            binding.progressBar.visibility = View.VISIBLE
            binding.contentLayout.alpha = 0.5f
            isSaving = true
            
            // Validate input fields before saving
            if (!validateInputFields()) {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.alpha = 1.0f
                isSaving = false
                return
            }
            
            // Create AdmobAds object from UI values
            val updatedAdmobAds = createAdmobAdsFromUI()
            
            // Save to Firebase
            adConfigRef.setValue(updatedAdmobAds)
                .addOnSuccessListener {
                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.alpha = 1.0f
                    isSaving = false
                    
                    Toast.makeText(this, "Ad configuration saved successfully", Toast.LENGTH_SHORT).show()
                    hasChanges = false
                }
                .addOnFailureListener { e ->
                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.alpha = 1.0f
                    isSaving = false
                    
                    Log.e("AdsControlActivity", "Error saving ad configuration", e)
                    Toast.makeText(this, "Error saving ad configuration: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            
        } catch (e: Exception) {
            // Hide loading indicator
            binding.progressBar.visibility = View.GONE
            binding.contentLayout.alpha = 1.0f
            isSaving = false
            
            Log.e("AdsControlActivity", "Error creating ad configuration", e)
            Toast.makeText(this, "Error saving ad configuration: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Add validation method to check input fields
    private fun validateInputFields(): Boolean {
        // Validate numeric fields
        try {
            if (binding.bannerAdsSwitch.isChecked) {
                binding.animationDurationEditText.text.toString().toLongOrNull() 
                    ?: return showValidationError("Animation duration must be a valid number")
                
                binding.bannerRetriesThresholdEditText.text.toString().toIntOrNull() 
                    ?: return showValidationError("Banner retries threshold must be a valid number")
            }
            
            if (binding.interstitialAdsSwitch.isChecked) {
                binding.interstitialDelayEditText.text.toString().toIntOrNull() 
                    ?: return showValidationError("Interstitial delay must be a valid number")
                
                binding.interstitialRetriesThresholdEditText.text.toString().toIntOrNull() 
                    ?: return showValidationError("Interstitial retries threshold must be a valid number")
            }
            
            if (binding.nativeAdsSwitch.isChecked) {
                binding.nativeAdAnimationDurationEditText.text.toString().toLongOrNull() 
                    ?: return showValidationError("Native ad animation duration must be a valid number")
            }
            
            // Validate ad IDs format if provided
            if (binding.bannerAdsSwitch.isChecked && binding.bannerAdIdEditText.text.toString().isNotEmpty()) {
                val bannerAdId = binding.bannerAdIdEditText.text.toString().trim()
                if (!isValidAdId(bannerAdId)) {
                    return showValidationError("Banner ad ID format is invalid")
                }
            }
            
            if (binding.interstitialAdsSwitch.isChecked && binding.interstitialAdIdEditText.text.toString().isNotEmpty()) {
                val interstitialAdId = binding.interstitialAdIdEditText.text.toString().trim()
                if (!isValidAdId(interstitialAdId)) {
                    return showValidationError("Interstitial ad ID format is invalid")
                }
            }
            
            if (binding.appOpenAdsSwitch.isChecked && binding.appOpenAdIdEditText.text.toString().isNotEmpty()) {
                val appOpenAdId = binding.appOpenAdIdEditText.text.toString().trim()
                if (!isValidAdId(appOpenAdId)) {
                    return showValidationError("App Open ad ID format is invalid")
                }
            }
            
            if (binding.nativeAdsSwitch.isChecked && binding.nativeAdIdEditText.text.toString().isNotEmpty()) {
                val nativeAdId = binding.nativeAdIdEditText.text.toString().trim()
                if (!isValidAdId(nativeAdId)) {
                    return showValidationError("Native ad ID format is invalid")
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e("AdsControlActivity", "Validation error", e)
            Toast.makeText(this, "Validation error: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    private fun showValidationError(message: String): Boolean {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        return false
    }
    
    private fun isValidAdId(adId: String): Boolean {
        // Basic validation for AdMob ID format
        return adId.isEmpty() || adId.startsWith("ca-app-pub-") && adId.contains("/")
    }
    
    private fun createAdmobAdsFromUI(): AdmobAds {
        return AdmobAds(
            // Ad IDs - only save if the switch is enabled
            bannerAd = if (binding.bannerAdsSwitch.isChecked) binding.bannerAdIdEditText.text.toString().trim() else null,
            interstitialAd = if (binding.interstitialAdsSwitch.isChecked) binding.interstitialAdIdEditText.text.toString().trim() else null,
            appOpenAd = if (binding.appOpenAdsSwitch.isChecked) binding.appOpenAdIdEditText.text.toString().trim() else null,
            nativeAd = if (binding.nativeAdsSwitch.isChecked) binding.nativeAdIdEditText.text.toString().trim() else null,
            
            // Ad enabled flags
            bannerAdEnabled = binding.bannerAdsSwitch.isChecked,
            interstitialAdEnabled = binding.interstitialAdsSwitch.isChecked,
            appOpenAdEnabled = binding.appOpenAdsSwitch.isChecked,
            nativeAdEnabled = binding.nativeAdsSwitch.isChecked,
            rewardedAdEnabled = binding.rewardedAdsSwitch.isChecked,
            
            // Banner settings
            bannerAdAnimation = binding.bannerAnimationSwitch.isChecked,
            bannerAdShowWaterMark = binding.watermarkSwitch.isChecked,
            bannerAdWaterMarkText = binding.watermarkTextEditText.text.toString().trim(),
            bannerAdWaterMarkBG = binding.watermarkBgColorEditText.text.toString().trim(),
            bannerAdWaterMarkFG = binding.watermarkFgColorEditText.text.toString().trim(),
            bannerAdAnimationDuration = binding.animationDurationEditText.text.toString().toLongOrNull() ?: 500L,
            bannerAdHideWaterMarkOnError = binding.hideWatermarkOnErrorSwitch.isChecked,
            bannerAdRetriesThreshold = binding.bannerRetriesThresholdEditText.text.toString().toIntOrNull() ?: 10,
            
            // Interstitial settings
            interstitialAdDelay = binding.interstitialDelayEditText.text.toString().toIntOrNull() ?: 3,
            interstitialAdRetriesThreshold = binding.interstitialRetriesThresholdEditText.text.toString().toIntOrNull() ?: 10,
            showInterstitialAdInstantly = binding.showInterstitialInstantlySwitch.isChecked,
            showProgressInterstitialAdInstantly = binding.showProgressInterstitialInstantlySwitch.isChecked,
            permenantHideInterstitialAdProgress = binding.hideInterstitialProgressSwitch.isChecked,
            loadInterstitialAdAgainAfterShowing = binding.loadInterstitialAgainSwitch.isChecked,
            
            // Dialog settings
            dialogMessage = binding.dialogMessageEditText.text.toString().trim(),
            dialogCancellable = binding.dialogCancellableSwitch.isChecked,
            
            // Native ad settings
            nativeAdShowShimmer = binding.nativeAdShimmerSwitch.isChecked,
            nativeAdAnimationDuration = binding.nativeAdAnimationDurationEditText.text.toString().toLongOrNull() ?: 500L
        )
    }
    
    private fun showBulkEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bulk_edit_ads, null)
        val prefixEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.prefixEditText)
        
        AlertDialog.Builder(this)
            .setTitle("Bulk Edit Ad IDs")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val prefix = prefixEditText.text.toString().trim()
                if (prefix.isNotEmpty()) {
                    applyPrefixToAllAdIds(prefix)
                    hasChanges = true
                } else {
                    Toast.makeText(this, "Please enter a valid publisher ID prefix", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    private fun applyPrefixToAllAdIds(prefix: String) {
        try {
            // Validate prefix format
            if (!prefix.startsWith("ca-app-pub-")) {
                Toast.makeText(this, "Publisher ID should start with 'ca-app-pub-'", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Get current ad IDs
            val bannerAdId = binding.bannerAdIdEditText.text.toString()
            val interstitialAdId = binding.interstitialAdIdEditText.text.toString()
            val appOpenAdId = binding.appOpenAdIdEditText.text.toString()
            val nativeAdId = binding.nativeAdIdEditText.text.toString()
            
            // Extract the numeric part of each ad ID (assuming format is ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY)
            val bannerNumeric = extractAdUnitId(bannerAdId)
            val interstitialNumeric = extractAdUnitId(interstitialAdId)
            val appOpenNumeric = extractAdUnitId(appOpenAdId)
            val nativeNumeric = extractAdUnitId(nativeAdId)
            
            // Apply the new prefix
            if (bannerNumeric.isNotEmpty()) {
                binding.bannerAdIdEditText.setText("$prefix/$bannerNumeric")
            }
            
            if (interstitialNumeric.isNotEmpty()) {
                binding.interstitialAdIdEditText.setText("$prefix/$interstitialNumeric")
            }
            
            if (appOpenNumeric.isNotEmpty()) {
                binding.appOpenAdIdEditText.setText("$prefix/$appOpenNumeric")
            }
            
            if (nativeNumeric.isNotEmpty()) {
                binding.nativeAdIdEditText.setText("$prefix/$nativeNumeric")
            }
            
            Toast.makeText(this, "Publisher ID prefix applied to all ad units", Toast.LENGTH_SHORT).show()
            hasChanges = true
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error applying prefix: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun extractAdUnitId(adId: String): String {
        return if (adId.contains("/")) {
            adId.substringAfterLast("/")
        } else if (adId.isNotEmpty() && !adId.startsWith("ca-app-pub-")) {
            // If it doesn't have a prefix but has content, assume it's just the unit ID
            adId
        } else {
            ""
        }
    }
    
    private fun applyTestAdIds() {
        // Set test ad IDs from Google's sample IDs
        binding.bannerAdIdEditText.setText("ca-app-pub-3940256099942544/6300978111")
        binding.interstitialAdIdEditText.setText("ca-app-pub-3940256099942544/1033173712")
        binding.appOpenAdIdEditText.setText("ca-app-pub-3940256099942544/9257395921")
        binding.nativeAdIdEditText.setText("ca-app-pub-3940256099942544/2247696110")
        
        // Enable all ad types
        binding.bannerAdsSwitch.isChecked = true
        binding.interstitialAdsSwitch.isChecked = true
        binding.appOpenAdsSwitch.isChecked = true
        binding.nativeAdsSwitch.isChecked = true
        
        // Set recommended settings for testing
        binding.interstitialDelayEditText.setText("1") // Lower delay for testing
        binding.showInterstitialInstantlySwitch.isChecked = true // Show instantly for testing
        
        Toast.makeText(this, "Test ad IDs applied. Don't forget to save!", Toast.LENGTH_SHORT).show()
        hasChanges = true
    }
    
    private fun showApplyToAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Apply to All Platforms")
            .setMessage("This will apply the current ad IDs to all platforms (Android, iOS, Web). Continue?")
            .setPositiveButton("Apply") { _, _ ->
                applyToAllPlatforms()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    private fun applyToAllPlatforms() {
        try {
            // Prevent multiple simultaneous operations
            if (isSaving) {
                Toast.makeText(this, "Operation in progress, please wait...", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Show loading indicator
            binding.progressBar.visibility = View.VISIBLE
            binding.contentLayout.alpha = 0.5f
            isSaving = true
            
            // Validate input fields before saving
            if (!validateInputFields()) {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.alpha = 1.0f
                isSaving = false
                return
            }
            
            // Create AdmobAds object from UI values
            val updatedAdmobAds = createAdmobAdsFromUI()
            
            // Save to Firebase for all platforms
            val updates = HashMap<String, Any>()
            updates["admob"] = updatedAdmobAds
            updates["ios_admob"] = updatedAdmobAds
            updates["web_admob"] = updatedAdmobAds
            
            database.reference.updateChildren(updates)
                .addOnSuccessListener {
                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.alpha = 1.0f
                    isSaving = false
                    
                    Toast.makeText(this, "Applied to all platforms successfully", Toast.LENGTH_SHORT).show()
                    hasChanges = false
                }
                .addOnFailureListener { e ->
                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.alpha = 1.0f
                    isSaving = false
                    
                    Toast.makeText(this, "Error applying to all platforms: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            
        } catch (e: Exception) {
            // Hide loading indicator
            binding.progressBar.visibility = View.GONE
            binding.contentLayout.alpha = 1.0f
            isSaving = false
            
            Toast.makeText(this, "Error applying to all platforms: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (hasChanges) {
                showUnsavedChangesDialog()
                return true
            }
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them before leaving?")
            .setPositiveButton("Save") { _, _ ->
                saveAdConfiguration()
                // Wait a bit for the save to complete with a timeout
                handler.postDelayed({
                    if (!isSaving) {
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    } else {
                        // If still saving, wait a bit more
                        handler.postDelayed({
                            finish()
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }, 1000)
                    }
                }, 1000)
            }
            .setNegativeButton("Discard") { _, _ ->
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            .setNeutralButton("Cancel", null)
            .create()
            .show()
    }
    
    override fun onBackPressed() {
        if (hasChanges) {
            showUnsavedChangesDialog()
            return
        }
        super.onBackPressed()
        // Apply a smooth transition animation when going back
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
} 