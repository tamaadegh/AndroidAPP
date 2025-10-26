package com.example.tamaade.data.repository

import com.example.tamaade.data.remote.api.RetrofitClient
import com.example.tamaade.data.remote.model.Product
import retrofit2.Response

class ProductRepository {
    suspend fun getProducts(): Response<List<Product>> {
        return RetrofitClient.apiService.getProducts()
    }
}
