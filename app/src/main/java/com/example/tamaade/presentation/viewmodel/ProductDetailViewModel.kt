package com.example.tamaade.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tamaade.data.local.AppDatabase
import com.example.tamaade.data.network.ProductDetailDto
import com.example.tamaade.data.repository.ProductRepository
import com.example.tamaade.data.remote.api.RetrofitClient
import kotlinx.coroutines.launch

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ProductRepository(
        apiService = RetrofitClient.instance,
        cartDao = database.cartDao(),
        favoriteDao = database.favoriteDao(),
        productDetailDao = database.productDetailDao()
    )

    private val _product = MutableLiveData<ProductDetailDto>()
    val product: LiveData<ProductDetailDto> = _product

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchProductDetails(slug: String) {
        viewModelScope.launch {
            try {
                val productDto = repository.getProductDetailCached(slug)
                
                if (productDto != null) {
                    _product.postValue(productDto)
                    checkIfFavorite(productDto.id)
                } else {
                    _error.postValue("Failed to fetch product details. Check internet connection.")
                }
            } catch (e: Exception) {
                _error.postValue("Exception: ${e.message}")
            }
        }
    }

    private fun checkIfFavorite(productId: Int) {
        viewModelScope.launch {
            _isFavorite.postValue(repository.isFavorite(productId))
        }
    }

    fun toggleFavorite() {
        val currentProduct = _product.value ?: return
        viewModelScope.launch {
            try {
                if (_isFavorite.value == true) {
                    repository.removeFromFavorites(currentProduct.id)
                    _isFavorite.postValue(false)
                } else {
                    repository.addToFavorites(currentProduct.id)
                    _isFavorite.postValue(true)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to update favorite: ${e.message}")
            }
        }
    }

    fun addToCart(productId: Int, quantity: Int) {
        viewModelScope.launch {
            try {
                 repository.addToCart(productId, quantity)
            } catch (e: Exception) {
                 _error.postValue("Failed to add to cart: ${e.message}")
            }
        }
    }
}
