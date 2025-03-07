package com.samyak.urltvadmin

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.samyak.urltvadmin.databinding.DialogEditCategoryBinding
import com.samyak.urltvadmin.models.Category
import com.samyak.urltvadmin.utils.ValidationUtils

class EditCategoryDialog(
    context: Context,
    private val category: Category,
    private val listener: CategoryUpdateListener
) : Dialog(context) {

    private val binding: DialogEditCategoryBinding = DialogEditCategoryBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupClickListeners()
        populateData()
    }

    private fun validateInputs(): Boolean {
        return ValidationUtils.validateRequired(
            binding.editTextCategoryName,
            context.getString(R.string.category_name_required)
        )
    }

    private fun setupClickListeners() {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        binding.buttonUpdate.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val name = binding.editTextCategoryName.text.toString().trim()
            
            category.apply {
                this.name = name
            }
            
            listener.onCategoryUpdated(category)
            dismiss()
        }
    }

    private fun populateData() {
        binding.editTextCategoryName.setText(category.name)
    }

    fun interface CategoryUpdateListener {
        fun onCategoryUpdated(category: Category)
    }
} 