package com.example.tamaade.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tamaade.data.local.CartDao
import com.example.tamaade.data.local.FavoriteDao
import com.example.tamaade.data.local.ProductDao
import com.example.tamaade.data.repository.ProductRepository

class ProductViewModelFactory(
    private val cartDao: CartDao,
    private val favoriteDao: FavoriteDao,
    private val productDao: ProductDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(ProductRepository(cartDao, favoriteDao, productDao)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}