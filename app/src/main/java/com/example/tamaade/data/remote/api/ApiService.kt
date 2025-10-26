package com.example.tamaade.data.remote.api

import com.example.tamaade.data.remote.model.Product
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("api/products/")
    suspend fun getProducts(): Response<List<Product>>
}
