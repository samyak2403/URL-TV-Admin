package com.samyak.urltvadmin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.samyak.urltvadmin.databinding.ActivityMainBinding
import com.samyak.urltvadmin.utils.CategoryManager
import com.samyak.urltvadmin.utils.NetworkManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var networkManager: NetworkManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize network manager
        networkManager = NetworkManager(this)
        setupNetworkMonitoring()
        
        // Apply staggered animations to UI elements
        animateUIElements()
        
        // Set touch listeners for hover effects
        setupCardTouchEffects(binding.adminCard)
        setupCardTouchEffects(binding.categoryCard)
        setupCardTouchEffects(binding.networkCard)
        setupCardTouchEffects(binding.aboutCard)
        setupCardTouchEffects(binding.pushNotificationsCard)
        
        // Set click listeners for cards with improved handling
        binding.adminCard.setOnClickListener {
            // Ensure we navigate to AdminActivity with proper animation
            animateAndNavigate(binding.adminCard, AdminActivity::class.java)
        }
        
        binding.categoryCard.setOnClickListener {
            // For now, navigate to AdminActivity
            // In the future, this should navigate to CategoryActivity
            animateAndNavigate(binding.categoryCard, AddCategoryActivity::class.java)
        }
        
        binding.networkCard.setOnClickListener {
            // Show checking state
            binding.networkStatusText.text = "Checking..."
            binding.networkProgressIndicator.visibility = View.VISIBLE
            binding.networkIcon.alpha = 0.5f
            binding.networkIcon.clearAnimation()
            
            // Use good.png for all statuses
            binding.networkIcon.setImageResource(R.drawable.good)
            binding.networkIcon.clearColorFilter()
            
            // Force a network check when clicked
            networkManager.checkInitialConnectionStatus()
            
            // Add a small vibration feedback if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        
        binding.aboutCard.setOnClickListener {
            // Navigate to AboutActivity with animation
            animateAndNavigate(binding.aboutCard, AboutActivity::class.java)
        }
        
        binding.pushNotificationsCard.setOnClickListener {
            // Navigate to PushNotificationsActivity with animation
            animateAndNavigate(binding.pushNotificationsCard, PushNotificationsActivity::class.java)
        }
        
        // Initialize CategoryManager with Firebase data
        CategoryManager.loadCategoriesFromFirebase {}
    }
    
    private fun setupNetworkMonitoring() {
        // Set initial UI state
        binding.networkStatusText.text = "Checking..."
        
        // Use good.png for all network statuses
        binding.networkIcon.setImageResource(R.drawable.good)
        binding.networkIcon.clearColorFilter()
        
        // Show progress indicator while checking
        binding.networkProgressIndicator.visibility = View.VISIBLE
        binding.networkIcon.alpha = 0.5f
        
        // Observe network status changes
        networkManager.connectionStatus.observe(this, Observer { status ->
            updateNetworkUI(status)
        })
        
        // Perform initial check
        networkManager.checkInitialConnectionStatus()
    }
    
    private fun updateNetworkUI(status: NetworkManager.ConnectionStatus?) {
        binding.networkIcon.clearAnimation()
        
        // Prepare status change animation
        val statusChangeAnim = AnimationUtils.loadAnimation(this, R.anim.network_status_change)
        
        when (status) {
            NetworkManager.ConnectionStatus.CONNECTED_EXCELLENT -> {
                // Hide progress indicator
                binding.networkProgressIndicator.visibility = View.GONE
                binding.networkIcon.alpha = 1.0f
                
                // Update text and use good.png for all statuses
                binding.networkStatusText.text = "Excellent"
                binding.networkIcon.setImageResource(R.drawable.excellent)
                binding.networkIcon.clearColorFilter()
                
                // Apply animation
                binding.networkIcon.startAnimation(statusChangeAnim)
            }
            NetworkManager.ConnectionStatus.CONNECTED_GOOD -> {
                // Hide progress indicator
                binding.networkProgressIndicator.visibility = View.GONE
                binding.networkIcon.alpha = 1.0f
                
                // Update text and use good.png for all statuses
                binding.networkStatusText.text = "Good"
                binding.networkIcon.setImageResource(R.drawable.good)
                binding.networkIcon.clearColorFilter()
                
                // Apply animation
                binding.networkIcon.startAnimation(statusChangeAnim)
            }
            NetworkManager.ConnectionStatus.CONNECTED_POOR -> {
                // Hide progress indicator
                binding.networkProgressIndicator.visibility = View.GONE
                binding.networkIcon.alpha = 1.0f
                
                // Update text and use good.png for all statuses
                binding.networkStatusText.text = "Poor"
                binding.networkIcon.setImageResource(R.drawable.poor)
                binding.networkIcon.clearColorFilter()
                
                // Apply animation
                binding.networkIcon.startAnimation(statusChangeAnim)
            }
            NetworkManager.ConnectionStatus.DISCONNECTED -> {
                // Hide progress indicator
                binding.networkProgressIndicator.visibility = View.GONE
                binding.networkIcon.alpha = 1.0f
                
                // Update text and use good.png for all statuses
                binding.networkStatusText.text = "Disconnected"
                binding.networkIcon.setImageResource(R.drawable.disconnected)
                binding.networkIcon.clearColorFilter()
                
                // Apply animation
                binding.networkIcon.startAnimation(statusChangeAnim)
                
                // Add a subtle pulse animation for disconnected state
                Handler(Looper.getMainLooper()).postDelayed({
                    val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.network_pulse)
                    binding.networkIcon.startAnimation(pulseAnim)
                }, 500)
            }
            NetworkManager.ConnectionStatus.CHECKING -> {
                // Show progress indicator
                binding.networkProgressIndicator.visibility = View.VISIBLE
                binding.networkIcon.alpha = 0.5f
                
                // Update text and use good.png for all statuses
                binding.networkStatusText.text = "Checking..."
                binding.networkIcon.setImageResource(R.drawable.good)
                binding.networkIcon.clearColorFilter()
            }
            else -> {
                // Hide progress indicator
                binding.networkProgressIndicator.visibility = View.GONE
                binding.networkIcon.alpha = 1.0f
                
                // Update text and use good.png for all statuses
                binding.networkStatusText.text = "Unknown"
                binding.networkIcon.setImageResource(R.drawable.unknown)
                binding.networkIcon.clearColorFilter()
            }
        }
    }
    
    private fun setupCardTouchEffects(card: View) {
        val hoverAnimation = AnimationUtils.loadAnimation(this, R.anim.card_hover)
        val reverseAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
        reverseAnimation.duration = 200
        
        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.startAnimation(hoverAnimation)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.startAnimation(reverseAnimation)
                    v.performClick() // Ensure click is registered
                    false
                }
                else -> false
            }
        }
    }
    
    private fun animateUIElements() {
        // Animate title with delay
        binding.titleTextView.visibility = View.INVISIBLE
        binding.subtitleTextView.visibility = View.INVISIBLE
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.titleTextView.visibility = View.VISIBLE
            val titleAnim = AnimationUtils.loadAnimation(this, R.anim.staggered_fade_in)
            binding.titleTextView.startAnimation(titleAnim)
            
            // Animate subtitle with a slight delay
            Handler(Looper.getMainLooper()).postDelayed({
                binding.subtitleTextView.visibility = View.VISIBLE
                val subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.staggered_fade_in)
                binding.subtitleTextView.startAnimation(subtitleAnim)
            }, 150)
        }, 300)
        
        // Animate cards with staggered timing
        binding.adminCard.visibility = View.INVISIBLE
        binding.categoryCard.visibility = View.INVISIBLE
        binding.networkCard.visibility = View.INVISIBLE
        binding.aboutCard.visibility = View.INVISIBLE
        binding.pushNotificationsCard.visibility = View.INVISIBLE
        
        // First row animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Animate admin card
            binding.adminCard.visibility = View.VISIBLE
            val adminCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
            binding.adminCard.startAnimation(adminCardAnim)
            
            // Animate category card with a slight delay
            Handler(Looper.getMainLooper()).postDelayed({
                binding.categoryCard.visibility = View.VISIBLE
                val categoryCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
                binding.categoryCard.startAnimation(categoryCardAnim)
            }, 150)
            
            // Second row animation with more delay
            Handler(Looper.getMainLooper()).postDelayed({
                binding.networkCard.visibility = View.VISIBLE
                val networkCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
                binding.networkCard.startAnimation(networkCardAnim)
                
                // Animate about card with a slight delay after network card
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.aboutCard.visibility = View.VISIBLE
                    val aboutCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
                    binding.aboutCard.startAnimation(aboutCardAnim)
                    
                    // Animate push notifications card with a slight delay after about card
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.pushNotificationsCard.visibility = View.VISIBLE
                        val pushNotificationsCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
                        binding.pushNotificationsCard.startAnimation(pushNotificationsCardAnim)
                    }, 150)
                }, 150)
            }, 300)
        }, 600)
    }
    
    private fun <T> animateAndNavigate(view: View, activityClass: Class<T>) {
        // Disable the view to prevent multiple clicks
        view.isEnabled = false
        
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.card_scale)
        view.startAnimation(scaleAnimation)
        
        // Delay the navigation to allow the animation to complete
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, activityClass)
            // Add any extra data if needed
            // intent.putExtra("key", "value")
            
            startActivity(intent)
            
            // Apply a smooth transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            // Re-enable the view (though the activity will be finishing)
            view.isEnabled = true
        }, 200)
    }
    
    private fun showAboutDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("About URL TV Admin")
            .setMessage("""
                Version: 1.0
                
                URL TV Admin is a powerful tool for managing your IPTV content. This admin panel allows you to:
                
                • Manage channel categories
                • Add and edit channels
                • Monitor network connectivity
                • Configure app settings
                
                © 2023 URL TV Admin. All rights reserved.
                
                For support: support@urltvadmin.com
            """.trimIndent())
            .setPositiveButton("OK", null)
            .create()
        
        dialog.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up network callback
        networkManager.unregisterNetworkCallback()
    }
}