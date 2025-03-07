package com.samyak.urltvadmin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samyak.urltvadmin.databinding.ActivityAddChannleBinding
import com.samyak.urltvadmin.utils.CategoryManager
import com.samyak.urltvadmin.utils.ValidationUtils
import com.samyak.urltvadmin.repository.ChannelRepository

class AddChannleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddChannleBinding
    private val channelRepository = ChannelRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddChannleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.add_channel)
        }
        
        // Set arrow color to white using DrawableCompat
        binding.toolbar.navigationIcon?.let { drawable ->
            drawable.setTint(getColor(android.R.color.white))
        }
        
        // Set title color to white
        binding.toolbar.setTitleTextColor(getColor(android.R.color.white))
        
        // Set status bar color to red
        window.statusBarColor = getColor(R.color.Red)
    }

    private fun setupCategoryDropdown() {
        val adapter = CategoryManager.getCategoryAdapter(this)
        binding.channelCategory.setAdapter(adapter)
        
        // Set default category if needed
        binding.channelCategory.setText(CategoryManager.categories.first(), false)
    }

    private fun setupClickListeners() {
        binding.buttonAddChannel.setOnClickListener { addChannel() }
    }

    private fun addChannel() {
        if (!validateInputs()) return

        val channel = Channel(
            name = binding.editTextChannelName.text.toString().trim(),
            link = binding.editTextChannelLink.text.toString().trim(),
            logo = binding.channleLogo.text.toString().trim(),
            category = binding.channelCategory.text.toString().trim()
        )

        showLoading(true)
        channelRepository.addChannel(channel)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, R.string.channel_added_successfully, Toast.LENGTH_SHORT).show()
                clearFields()
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.failed_to_add_channel, Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.editTextChannelName.text.toString().trim().isEmpty()) {
            binding.editTextChannelName.error = getString(R.string.channel_name_required)
            isValid = false
        }

        if (!ValidationUtils.validateUrl(binding.editTextChannelLink, getString(R.string.channel_link_required))) {
            isValid = false
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.buttonAddChannel.isEnabled = !show
        // Add loading indicator if needed
    }

    private fun clearFields() {
        binding.editTextChannelName.setText("")
        binding.editTextChannelLink.setText("")
        binding.channleLogo.setText("")
        binding.channelCategory.setText("")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}