package com.samyak.urltvadmin.utils

import android.content.Context
import android.widget.ArrayAdapter

object CategoryManager {
    const val ALL_CATEGORIES = "All Categories"
    
    val categories = listOf(ALL_CATEGORIES) + listOf(
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

    fun getCategoryAdapter(context: Context) = 
        ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)

    fun isValidCategory(category: String): Boolean =
        categories.contains(category) || category == ALL_CATEGORIES
} 