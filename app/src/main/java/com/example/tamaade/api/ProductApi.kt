package com.example.tamaade.api

import com.example.tamaade.data.remote.model.Product
import retrofit2.http.GET

interface ProductApi {
    @GET("product/storefront/api/products/")
    suspend fun getProducts(): List<Product>
}