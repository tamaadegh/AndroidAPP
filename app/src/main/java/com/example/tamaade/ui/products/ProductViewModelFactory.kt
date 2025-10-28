package com.example.tamaade.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tamaade.data.local.CartDao
import com.example.tamaade.data.local.FavoriteDao
import com.example.tamaade.data.repository.ProductRepository

class ProductViewModelFactory(private val cartDao: CartDao, private val favoriteDao: FavoriteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(ProductRepository(cartDao, favoriteDao)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}