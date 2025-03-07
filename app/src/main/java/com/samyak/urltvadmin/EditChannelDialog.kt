package com.samyak.urltvadmin

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.samyak.urltvadmin.databinding.DialogEditChannelBinding
import com.samyak.urltvadmin.utils.CategoryManager
import com.samyak.urltvadmin.utils.ValidationUtils

class EditChannelDialog(
    context: Context,
    private val channel: Channel,
    private val listener: ChannelUpdateListener
) : Dialog(context) {

    private val binding: DialogEditChannelBinding = DialogEditChannelBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupCategoryDropdown()
        setupClickListeners()
        populateData()
    }

    private fun setupCategoryDropdown() {
        val categories = CategoryManager.categories
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        binding.editTextChannelCategory.setAdapter(adapter)
        
        // Set current category
        binding.editTextChannelCategory.setText(channel.category, false)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.editTextChannelName.text.toString().trim().isEmpty()) {
            binding.editTextChannelName.error = context.getString(R.string.channel_name_required)
            isValid = false
        }

        if (!ValidationUtils.validateUrl(binding.editTextChannelLink, context.getString(R.string.channel_link_required))) {
            isValid = false
        }

        return isValid
    }

    private fun setupClickListeners() {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        binding.buttonUpdate.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val name = binding.editTextChannelName.text.toString().trim()
            val link = binding.editTextChannelLink.text.toString().trim()
            val logo = binding.editTextChannelLogo.text.toString().trim()
            val category = binding.editTextChannelCategory.text.toString().trim()

            channel.apply {
                this.name = name
                this.link = link
                this.logo = logo
                this.category = category
            }
            
            listener.onChannelUpdated(channel)
            dismiss()
        }
    }

    private fun populateData() {
        binding.editTextChannelName.setText(channel.name)
        binding.editTextChannelLink.setText(channel.link)
        binding.editTextChannelLogo.setText(channel.logo)
        binding.editTextChannelCategory.setText(channel.category)
    }

    fun interface ChannelUpdateListener {
        fun onChannelUpdated(channel: Channel)
    }
}