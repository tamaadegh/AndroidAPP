package com.example.tamaade.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tamaade.data.network.ProductListItemDto
import com.example.tamaade.data.repository.ProductRepository
import com.example.tamaade.data.remote.api.RetrofitClient
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val repository = ProductRepository(RetrofitClient.instance)

    private val _products = MutableLiveData<List<ProductListItemDto>>()
    val products: LiveData<List<ProductListItemDto>> = _products

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchProducts() {
        viewModelScope.launch {
            try {
                val response = repository.getProducts()
                if (response.isSuccessful) {
                    _products.postValue(response.body()?.results)
                } else {
                    _error.postValue("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.postValue("Exception: ${e.message}")
            }
        }
    }
}
