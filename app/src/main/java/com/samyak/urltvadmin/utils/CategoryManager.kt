package com.samyak.urltvadmin.utils

import android.content.Context
import android.widget.ArrayAdapter
import com.samyak.urltvadmin.models.Category
import com.samyak.urltvadmin.repository.CategoryRepository

object CategoryManager {
    const val ALL_CATEGORIES = "All Categories"
    
    // Default categories
    private val defaultCategories = listOf(
        "News",
        "Sports",
        "Entertainment",
        "Movies",
        "Music",
        "Kids",
        "Documentary",
        "Religious",
        "Education",
        "Other"
    )
    
    // This will be populated from Firebase
    private val dynamicCategories = mutableListOf<String>()
    
    // Combined categories for display
    val categories: List<String>
        get() = listOf(ALL_CATEGORIES) + dynamicCategories.ifEmpty { defaultCategories }

    fun getCategoryAdapter(context: Context) = 
        ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)

    fun isValidCategory(category: String): Boolean =
        categories.contains(category) || category == ALL_CATEGORIES
    
    fun loadCategoriesFromFirebase(onComplete: () -> Unit) {
        val categoryRepository = CategoryRepository()
        categoryRepository.getAllCategories()
            .addOnSuccessListener { result ->
                dynamicCategories.clear()
                dynamicCategories.addAll(result.map { it.name })
                onComplete()
            }
            .addOnFailureListener {
                // If loading fails, we'll use default categories
                dynamicCategories.clear()
                onComplete()
            }
    }
} 