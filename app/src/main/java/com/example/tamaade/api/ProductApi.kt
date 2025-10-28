package com.example.tamaade.api

import com.example.tamaade.data.remote.model.Category
import com.example.tamaade.data.remote.model.Product
import retrofit2.http.GET

interface ProductApi {
    @GET("api/products/")
    suspend fun getProducts(): List<Product>

    @GET("api/products/categories/")
    suspend fun getCategories(): List<Category>
}