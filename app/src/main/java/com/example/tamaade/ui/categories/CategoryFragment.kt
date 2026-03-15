package com.example.tamaade.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tamaade.data.repository.CategoryRepository
import kotlinx.coroutines.launch

/**
 * Example Fragment showing how to fetch and display categories from GraphQL API
 */
class CategoryFragment : Fragment() {
    
    private val categoryRepository = CategoryRepository()
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate your layout here
        val view = inflater.inflate(android.R.layout.simple_list_item_1, container, false)
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(android.R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Fetch categories
        fetchCategories()
        
        return view
    }
    
    private fun fetchCategories() {
        lifecycleScope.launch {
            // Show loading indicator
            showLoading(true)
            
            // Fetch categories with custom limit
            val result = categoryRepository.getCategories(first = 20)
            
            result.onSuccess { categories ->
                // Hide loading
                showLoading(false)
                
                // Update UI with categories
                categories.forEach { category ->
                    println("Category: ${category.name} - ${category.description}")
                }
                
                // Set adapter with categories
                // recyclerView.adapter = CategoryAdapter(categories)
                
            }.onFailure { error ->
                // Hide loading
                showLoading(false)
                
                // Show error message
                Toast.makeText(
                    requireContext(),
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        // Implement your loading indicator logic
    }
}
