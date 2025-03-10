package com.samyak.urltvadmin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.onesignal.OneSignal
import com.samyak.urltvadmin.databinding.ActivityPushNotificationsBinding
import com.samyak.urltvadmin.utils.CategoryManager
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class PushNotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPushNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    
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
    }
    
    private fun loadCategories() {
        // Show loading indicator
        binding.categoryProgressBar.visibility = View.VISIBLE
        
        // Load categories from CategoryManager
        CategoryManager.loadCategoriesFromFirebase { categoriesList ->
            // Add "All Users" option at the beginning
            val categoryOptions = mutableListOf("All Users")
            
            // Cast the categoriesList to List<String> and add all items
            @Suppress("UNCHECKED_CAST")
            val categories = categoriesList as? List<String> ?: CategoryManager.categories
            categoryOptions.addAll(categories.filter { it != CategoryManager.ALL_CATEGORIES })
            
            // Setup adapter for spinner
            val adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categoryOptions
            )
            
            binding.categorySpinner.adapter = adapter
            binding.categoryProgressBar.visibility = View.GONE
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
                    
                    // Save to history
                    saveNotificationToHistory(title, message, targetCategory)
                    
                    Toast.makeText(
                        this,
                        "Notification sent successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Refresh history
                    loadNotificationHistory()
                    
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
        val notification = hashMapOf(
            "title" to title,
            "message" to message,
            "targetCategory" to targetCategory,
            "timestamp" to timestamp
        )
        
        db.collection("notification_history")
            .add(notification)
            .addOnSuccessListener {
                // Successfully saved to history
            }
            .addOnFailureListener { e ->
                // Failed to save to history
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
                        val title = doc.getString("title") ?: ""
                        val message = doc.getString("message") ?: ""
                        val targetCategory = doc.getString("targetCategory") ?: "All Users"
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        val date = dateFormat.format(Date(timestamp))
                        
                        NotificationHistoryItem(title, message, targetCategory, date)
                    }
                    
                    val adapter = NotificationHistoryAdapter(this, historyItems)
                    binding.historyList.adapter = adapter
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
    
    data class NotificationHistoryItem(
        val title: String,
        val message: String,
        val targetCategory: String,
        val date: String
    )
    
    inner class NotificationHistoryAdapter(
        private val context: android.content.Context,
        private val items: List<NotificationHistoryItem>
    ) : android.widget.ArrayAdapter<NotificationHistoryItem>(
        context,
        android.R.layout.simple_list_item_2,
        items
    ) {
        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val view = convertView ?: android.view.LayoutInflater.from(context)
                .inflate(R.layout.item_notification_history, parent, false)
            
            val item = items[position]
            
            view.findViewById<android.widget.TextView>(R.id.titleText).text = item.title
            view.findViewById<android.widget.TextView>(R.id.messageText).text = item.message
            view.findViewById<android.widget.TextView>(R.id.targetText).text = "To: ${item.targetCategory}"
            view.findViewById<android.widget.TextView>(R.id.dateText).text = item.date
            
            return view
        }
    }
} 