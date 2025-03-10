package com.samyak.urltvadmin

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DefaultItemAnimator
import com.samyak.urltvadmin.adapters.CategoryAdapter
import com.samyak.urltvadmin.databinding.ActivityAddCategoryBinding
import com.samyak.urltvadmin.models.Category
import com.samyak.urltvadmin.repository.CategoryRepository
import com.samyak.urltvadmin.utils.CategoryManager
import com.samyak.urltvadmin.utils.ValidationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCategoryBinding
    private val categoryRepository = CategoryRepository()
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set status bar color to colorPrimary
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadCategories()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.manage_categories)
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(categories, 
            onEditClick = { category -> showEditCategoryDialog(category) },
            onDeleteClick = { category -> deleteCategory(category) }
        )
        
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(this@AddCategoryActivity)
            adapter = categoryAdapter
            itemAnimator = DefaultItemAnimator()
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        }
    }

    private fun setupClickListeners() {
        binding.buttonAddCategory.setOnClickListener { addCategory() }
    }

    private fun loadCategories() {
        showLoading(true)
        categoryRepository.getAllCategories()
            .addOnSuccessListener { result ->
                showLoading(false)
                categories.clear()
                categories.addAll(result)
                categoryAdapter.notifyDataSetChanged()
                
                // Update empty state visibility
                updateEmptyState()
                binding.recyclerViewCategories.scheduleLayoutAnimation()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.failed_to_load_categories, Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
    }
    
    private fun updateEmptyState() {
        if (categories.isEmpty()) {
            binding.recyclerViewCategories.visibility = View.GONE
            binding.textViewEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewCategories.visibility = View.VISIBLE
            binding.textViewEmptyState.visibility = View.GONE
        }
    }

    private fun addCategory() {
        if (!validateInputs()) return

        val categoryName = binding.editTextCategoryName.text.toString().trim()
        val category = Category(name = categoryName)

        showLoading(true)
        categoryRepository.addCategory(category)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, R.string.category_added_successfully, Toast.LENGTH_SHORT).show()
                clearFields()
                loadCategories() // Refresh the list
                
                // Refresh CategoryManager
                CategoryManager.loadCategoriesFromFirebase {}
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.failed_to_add_category, Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditCategoryDialog(category: Category) {
        EditCategoryDialog(this, category) { updatedCategory ->
            updateCategory(updatedCategory)
        }.show()
    }

    private fun updateCategory(category: Category) {
        showLoading(true)
        categoryRepository.updateCategory(category)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, R.string.category_updated_successfully, Toast.LENGTH_SHORT).show()
                loadCategories() // Refresh the list
                
                // Refresh CategoryManager
                CategoryManager.loadCategoriesFromFirebase {}
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.failed_to_update_category, Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCategory(category: Category) {
        // Show confirmation dialog
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_category)
            .setMessage(getString(R.string.delete_category_confirmation, category.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                performDeleteCategory(category)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performDeleteCategory(category: Category) {
        showLoading(true)
        categoryRepository.deleteCategory(category.id)
            .addOnSuccessListener {
                showLoading(false)
                // Show success message with animation
                val successAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                binding.root.startAnimation(successAnimation)
                
                Toast.makeText(this, R.string.category_deleted_successfully, Toast.LENGTH_SHORT).show()
                loadCategories() // Refresh the list
                
                // Refresh CategoryManager
                CategoryManager.loadCategoriesFromFirebase {}
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, R.string.failed_to_delete_category, Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        return ValidationUtils.validateRequired(
            binding.editTextCategoryName,
            getString(R.string.category_name_required)
        )
    }

    private fun showLoading(show: Boolean) {
        binding.buttonAddCategory.isEnabled = !show
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun clearFields() {
        binding.editTextCategoryName.setText("")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun animateNewItem() {
        if (binding.recyclerViewCategories.adapter?.itemCount ?: 0 > 0) {
            val layoutManager = binding.recyclerViewCategories.layoutManager as LinearLayoutManager
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
            if (lastVisiblePosition >= 0) {
                val viewToAnimate = layoutManager.findViewByPosition(lastVisiblePosition)
                viewToAnimate?.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
                )
            }
        }
    }

    // Helper function to determine if a color is dark
    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
} 