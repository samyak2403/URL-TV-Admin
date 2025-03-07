package com.samyak.urltvadmin.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.tasks.TaskCompletionSource
import com.samyak.urltvadmin.models.Category

class CategoryRepository {
    private val database = FirebaseDatabase.getInstance()
    private val categoriesRef = database.getReference("categories")

    fun getAllCategories(): Task<List<Category>> {
        val taskCompletionSource = TaskCompletionSource<List<Category>>()
        
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = mutableListOf<Category>()
                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.getValue(Category::class.java)
                    category?.id = categorySnapshot.key ?: ""
                    if (category != null) {
                        categories.add(category)
                    }
                }
                taskCompletionSource.setResult(categories)
            }

            override fun onCancelled(error: DatabaseError) {
                taskCompletionSource.setException(error.toException())
            }
        })
        
        return taskCompletionSource.task
    }

    fun addCategory(category: Category): Task<Void> {
        val key = categoriesRef.push().key ?: return TaskCompletionSource<Void>().task
        category.id = key
        return categoriesRef.child(key).setValue(category)
    }

    fun updateCategory(category: Category): Task<Void> {
        return categoriesRef.child(category.id).setValue(category)
    }

    fun deleteCategory(categoryId: String): Task<Void> {
        return categoriesRef.child(categoryId).removeValue()
    }
} 