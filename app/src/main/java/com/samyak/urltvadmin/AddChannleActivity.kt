package com.samyak.urltvadmin

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import com.samyak.urltvadmin.utils.CategoryManager
import com.samyak.urltvadmin.utils.ValidationUtils
import com.samyak.urltvadmin.repository.ChannelRepository

class AddChannleActivity : AppCompatActivity() {
    private lateinit var editTextChannelName: TextInputEditText
    private lateinit var editTextChannelLink: TextInputEditText
    private lateinit var editTextChannelLogo: TextInputEditText
    private lateinit var channelCategory: AutoCompleteTextView
    private lateinit var buttonAddChannel: MaterialButton
    private lateinit var toolbar: Toolbar
    private val channelRepository = ChannelRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_channle)

        initializeViews()
        setupToolbar()
        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun initializeViews() {
        editTextChannelName = findViewById(R.id.editTextChannelName)
        editTextChannelLink = findViewById(R.id.editTextChannelLink)
        editTextChannelLogo = findViewById(R.id.channle_logo)
        channelCategory = findViewById(R.id.channelCategory)
        buttonAddChannel = findViewById(R.id.buttonAddChannel)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.add_channel)
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = CategoryManager.getCategoryAdapter(this)
        channelCategory.setAdapter(adapter)
        
        // Set default category if needed
        channelCategory.setText(CategoryManager.categories.first(), false)
    }

    private fun setupClickListeners() {
        buttonAddChannel.setOnClickListener { addChannel() }
    }

    private fun addChannel() {
        if (!validateInputs()) return

        val channel = Channel(
            name = editTextChannelName.text.toString().trim(),
            link = editTextChannelLink.text.toString().trim(),
            logo = editTextChannelLogo.text.toString().trim(),
            category = channelCategory.text.toString().trim()
        )

        channelRepository.addChannel(channel)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.channel_added_successfully, Toast.LENGTH_SHORT).show()
                clearFields()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.failed_to_add_channel, Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (editTextChannelName.text.toString().trim().isEmpty()) {
            editTextChannelName.error = getString(R.string.channel_name_required)
            isValid = false
        }

        if (!ValidationUtils.validateUrl(editTextChannelLink, getString(R.string.channel_link_required))) {
            isValid = false
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        buttonAddChannel.isEnabled = !show
        // Add loading indicator if needed
    }

    private fun clearFields() {
        editTextChannelName.setText("")
        editTextChannelLink.setText("")
        editTextChannelLogo.setText("")
        channelCategory.setText("")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}