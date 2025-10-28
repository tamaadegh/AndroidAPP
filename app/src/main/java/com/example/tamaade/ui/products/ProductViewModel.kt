package com.example.tamaade.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tamaade.data.remote.model.Category
import com.example.tamaade.data.remote.model.Product
import com.example.tamaade.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    val favoriteProductIds: LiveData<Set<Int>> = repository.getFavoriteItems().asLiveData(viewModelScope.coroutineContext).let {
        val mediator = MediatorLiveData<Set<Int>>()
        mediator.addSource(it) { items ->
            mediator.value = items.map { it.productId }.toSet()
        }
        mediator
    }

    init {
        fetchProducts()
        fetchCategories()
    }

    fun fetchProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _products.value = repository.getProducts()
            } catch (e: Exception) {
                _errorMessage.value = "Network Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            try {
                _categories.value = repository.getCategories()
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    fun addToCart(productId: Int, quantity: Int) {
        viewModelScope.launch {
            repository.addToCart(productId, quantity)
        }
    }

    fun toggleFavorite(productId: Int) {
        viewModelScope.launch {
            if (repository.isFavorite(productId)) {
                repository.removeFromFavorites(productId)
            } else {
                repository.addToFavorites(productId)
            }
        }
    }
}