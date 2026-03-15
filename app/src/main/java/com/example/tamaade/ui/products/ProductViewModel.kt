package com.example.tamaade.ui.products

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tamaade.data.remote.model.Category
import com.example.tamaade.data.remote.model.Product
import com.example.tamaade.data.repository.ProductRepository
import com.example.tamaade.data.network.ProductListResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    // LiveData from DB (Auto-updates when DB changes)
    val products: LiveData<List<Product>> = repository.getProductsFlow().asLiveData()

    // Favorites from DB
    val favoriteProducts: LiveData<List<Product>> = repository.getFavoriteProductsFlow().asLiveData()

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isMoreLoading = MutableLiveData<Boolean>()
    val isMoreLoading: LiveData<Boolean> = _isMoreLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var searchJob: Job? = null
    private var currentPage = 0
    private val pageSize = 20

    // Access to IDs if needed for toggle checks in UI efficiently
    val favoriteProductIds: LiveData<Set<Int>> = repository.getFavoriteItems().asLiveData(viewModelScope.coroutineContext).let {
        val mediator = MediatorLiveData<Set<Int>>()
        mediator.addSource(it) { items ->
            mediator.value = items.map { it.productId }.toSet()
        }
        mediator
    }

    init {
        // Initial fetch to populate cache if empty or stale
        refreshProducts()
        fetchCategories()
    }

    fun refreshProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                currentPage = 0
                repository.refreshProducts(limit = pageSize, offset = 0)
            } catch (e: Exception) {
                _errorMessage.value = "Network Error: ${e.message}"
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    fun loadMoreProducts() {
        if (_isMoreLoading.value == true) return
        
        viewModelScope.launch {
            _isMoreLoading.value = true
            try {
                currentPage++
                repository.refreshProducts(limit = pageSize, offset = currentPage * pageSize)
            } catch (e: Exception) {
                // Silent fail for load more, or show snackbar
                // currentPage-- 
            }
            _isMoreLoading.value = false
        }
    }
    
    fun searchProducts(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {
            if (query.isEmpty()) refreshProducts()
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _isLoading.value = true
            _errorMessage.value = null
            try {
                 repository.refreshProducts(search = query)
                 // Note: search results should probably be observed from a separate LiveData or filtered in DB
                 // Current implementation refreshes main list with search results if we used the same table.
                 // Ideally, search should use a different flow or the UI should observe a search-specific flow.
                 // For now, assuming refreshProducts patches the main table, the UI updates.
            } catch (e: Exception) {
                _errorMessage.value = "Search Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            try {
                // Using simple GraphQL client (no Apollo code generation needed)
                val categoryRepository = com.example.tamaade.data.repository.SimpleCategoryRepository()
                val result = categoryRepository.getCategories(first = 10)
                
                result.onSuccess { categories ->
                    _categories.value = categories
                }.onFailure { error ->
                    _errorMessage.value = "Failed to load categories: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading categories: ${e.message}"
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