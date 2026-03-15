package com.example.tamaade.data.network

import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApiService {

    @GET("product/storefront/api/products/")
    suspend fun getProducts(): ProductListResponse

    @GET("product/storefront/api/products/{slug}/")
    suspend fun getProductDetail(@Path("slug") slug: String): ProductDetailDto
}