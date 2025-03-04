package com.samyak.urltvadmin

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import com.samyak.urltvadmin.utils.CategoryManager

class EditChannelDialog(
    context: Context,
    private val channel: Channel,
    private val listener: ChannelUpdateListener
) : Dialog(context) {

    private lateinit var editTextChannelName: TextInputEditText
    private lateinit var editTextChannelLink: TextInputEditText
    private lateinit var editTextChannelLogo: TextInputEditText
    private lateinit var editTextChannelCategory: AutoCompleteTextView
    private lateinit var buttonUpdate: MaterialButton
    private lateinit var buttonCancel: MaterialButton

    init {
        setContentView(R.layout.dialog_edit_channel)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        initializeViews()
        setupCategoryDropdown()
        setupClickListeners()
        populateData()
    }

    private fun initializeViews() {
        editTextChannelName = findViewById(R.id.editTextChannelName)
        editTextChannelLink = findViewById(R.id.editTextChannelLink)
        editTextChannelLogo = findViewById(R.id.editTextChannelLogo)
        editTextChannelCategory = findViewById(R.id.editTextChannelCategory)
        buttonUpdate = findViewById(R.id.buttonUpdate)
        buttonCancel = findViewById(R.id.buttonCancel)
    }

    private fun setupCategoryDropdown() {
        val categories = CategoryManager.categories
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        editTextChannelCategory.setAdapter(adapter)
        
        // Set current category
        editTextChannelCategory.setText(channel.category, false)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (editTextChannelName.text.toString().trim().isEmpty()) {
            editTextChannelName.error = context.getString(R.string.channel_name_required)
            isValid = false
        }

        if (editTextChannelLink.text.toString().trim().isEmpty()) {
            editTextChannelLink.error = context.getString(R.string.channel_link_required)
            isValid = false
        }

        return isValid
    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener {
            dismiss()
        }

        buttonUpdate.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val name = editTextChannelName.text.toString().trim()
            val link = editTextChannelLink.text.toString().trim()
            val logo = editTextChannelLogo.text.toString().trim()
            val category = editTextChannelCategory.text.toString().trim()

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
        editTextChannelName.setText(channel.name)
        editTextChannelLink.setText(channel.link)
        editTextChannelLogo.setText(channel.logo)
        editTextChannelCategory.setText(channel.category)
    }

    fun interface ChannelUpdateListener {
        fun onChannelUpdated(channel: Channel)
    }
}