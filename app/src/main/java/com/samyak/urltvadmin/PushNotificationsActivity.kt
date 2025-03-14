package com.samyak.urltvadmin


import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.samyak.urltvadmin.adapters.NotificationHistoryAdapter
import com.samyak.urltvadmin.databinding.ActivityPushNotificationsBinding
import com.samyak.urltvadmin.models.NotificationHistoryItem
import com.samyak.urltvadmin.utils.CategoryManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PushNotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPushNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var notificationAdapter: NotificationHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityPushNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Push Notifications"
        
        // Initialize adapter with empty list
        notificationAdapter = NotificationHistoryAdapter(this, emptyList())
        binding.historyList.adapter = notificationAdapter

        // Load notification history
        loadNotificationHistory()
        
        // Setup send button
        binding.sendButton.setOnClickListener {
            if (validateInputs()) {
                sendPushNotification()
            }
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)


        // Setup category spinner
        loadCategories()

        // Add image URL validation
        binding.imageUrlEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateImageUrl(s.toString())
            }
        })
    }
    
    private fun loadCategories() {
        binding.categoryProgressBar.visibility = View.VISIBLE
        
        CategoryManager.loadCategoriesFromFirebase { categoriesList ->
            runOnUiThread {
                try {
                    // Create immutable list with "All Users" first
                    val categoryOptions = buildList {
                        add("All Users")
                        @Suppress("UNCHECKED_CAST")
                        (categoriesList as? List<String>)?.let { categories ->
                            addAll(categories.filter { it.isNotEmpty() && it != "All Users" }.sorted())
                        }
                    }
                    
                    // Custom adapter with better layout handling
                    val adapter = object : android.widget.ArrayAdapter<String>(
                        this@PushNotificationsActivity,
                        R.layout.item_spinner,
                        categoryOptions
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return (super.getView(position, convertView, parent) as TextView).apply {
                                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                                textSize = 16f
                            }
                        }
                        
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                                setPadding(24, 16, 24, 16)
                            }
                        }
                    }
                    
                    binding.categorySpinner.adapter = adapter
                } catch (e: Exception) {
                    Toast.makeText(this@PushNotificationsActivity, 
                        "Error loading categories: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
                binding.categoryProgressBar.visibility = View.GONE
            }
        }
    }
    
    private fun validateImageUrl(url: String) {
        if (url.isNotEmpty() && !android.util.Patterns.WEB_URL.matcher(url).matches()) {
            binding.imageUrlEditText.error = "Please enter a valid URL"
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Check title
        if (binding.titleEditText.text.toString().trim().isEmpty()) {
            binding.titleEditText.error = "Title is required"
            isValid = false
        }
        
        // Check message
        if (binding.messageEditText.text.toString().trim().isEmpty()) {
            binding.messageEditText.error = "Message is required"
            isValid = false
        }

        // Validate image URL if provided
        val imageUrl = binding.imageUrlEditText.text.toString().trim()
        if (imageUrl.isNotEmpty() && !android.util.Patterns.WEB_URL.matcher(imageUrl).matches()) {
            binding.imageUrlEditText.error = "Please enter a valid URL"
            isValid = false
        }
        
        return isValid
    }
    
    private fun sendPushNotification() {
        // Show loading state
        binding.sendButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        val title = binding.titleEditText.text.toString().trim()
        val message = binding.messageEditText.text.toString().trim()
        val targetCategory = binding.categorySpinner.selectedItem.toString()
        
        try {
            // Create notification content
            val notificationContent = JSONObject().apply {
                put("app_id", URLTVAdminApp.ONESIGNAL_APP_ID)
                put("contents", JSONObject().put("en", message))
                put("headings", JSONObject().put("en", title))
                
                // Add image if URL is provided
                val imageUrl = binding.imageUrlEditText.text.toString().trim()
                if (imageUrl.isNotEmpty()) {
                    put("big_picture", imageUrl)
                    put("large_icon", imageUrl)
                }

                if (targetCategory != "All Users") {
                    // Target specific category
                    put("filters", JSONArray().put(JSONObject().apply {
                        put("field", "tag")
                        put("key", "category")
                        put("relation", "=")
                        put("value", targetCategory)
                    }))
                } else {
                    // Target all users
                    put("included_segments", JSONArray().put("All"))
                }
            }
            
            val request = object : JsonObjectRequest(
                Request.Method.POST,
                "https://onesignal.com/api/v1/notifications",
                notificationContent,
                { response ->
                    // Success handler
                    binding.titleEditText.text?.clear()
                    binding.messageEditText.text?.clear()
                    binding.categorySpinner.setSelection(0)
                    binding.imageUrlEditText.text?.clear()
                    
                    // Save to history
                    saveNotificationToHistory(title, message, targetCategory)
                    
                    Toast.makeText(
                        this,
                        "Notification sent successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Reset UI state
                    binding.sendButton.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                },
                { error ->
                    // Error handler
                    Toast.makeText(
                        this,
                        "Failed to send notification: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Reset UI state
                    binding.sendButton.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Basic os_v2_app_xiokw67guzhfjed3y33kwlvzazljq5lmmjcuup5syk4pnszqgrltsivzewcyjz7eytuuee6eniy7q6qk3em7triqkttrelovpn3ulwq"
                    )
                }
            }
            
            Volley.newRequestQueue(this).add(request)
            
        } catch (e: JSONException) {
            // Handle JSON error
            binding.sendButton.isEnabled = true
            binding.progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                "Error creating notification: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun saveNotificationToHistory(title: String, message: String, targetCategory: String) {
        val timestamp = System.currentTimeMillis()
        val imageUrl = binding.imageUrlEditText.text.toString().trim()
        
        val notification = hashMapOf(
            "title" to title,
            "message" to message,
            "targetCategory" to targetCategory,
            "timestamp" to timestamp,
            "imageUrl" to imageUrl
        )
        
        db.collection("notification_history")
            .add(notification)
            .addOnSuccessListener {
                // Refresh the history list after successful save
                loadNotificationHistory()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to save notification history: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    
    private fun loadNotificationHistory() {
        binding.historyProgressBar.visibility = View.VISIBLE
        binding.emptyHistoryText.visibility = View.GONE
        binding.historyList.visibility = View.GONE
        
        db.collection("notification_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.emptyHistoryText.visibility = View.VISIBLE
                    binding.historyList.visibility = View.GONE
                } else {
                    binding.emptyHistoryText.visibility = View.GONE
                    binding.historyList.visibility = View.VISIBLE
                    
                    val historyItems = documents.map { doc ->
                        NotificationHistoryItem(
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            targetCategory = doc.getString("targetCategory") ?: "All Users",
                            date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(doc.getLong("timestamp") ?: 0L)),
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    }
                    
                    // Update adapter with new items
                    notificationAdapter = NotificationHistoryAdapter(this, historyItems)
                    binding.historyList.adapter = notificationAdapter
                }
                
                binding.historyProgressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.historyProgressBar.visibility = View.GONE
                binding.emptyHistoryText.visibility = View.VISIBLE
                binding.emptyHistoryText.text = "Failed to load history: ${e.message}"
            }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 