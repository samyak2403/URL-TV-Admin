package com.samyak.urltvadmin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.samyak.urltvadmin.databinding.ItemCategoryBinding
import com.samyak.urltvadmin.models.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: Category) {
            binding.textViewCategoryName.text = category.name
            
            binding.buttonEdit.setOnClickListener {
                onEditClick(category)
            }
            
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(category)
            }
        }
    }
} 