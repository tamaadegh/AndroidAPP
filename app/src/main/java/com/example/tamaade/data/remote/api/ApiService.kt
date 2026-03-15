package com.example.tamaade.data.remote.api

import com.example.tamaade.data.network.CategoryDto
import com.example.tamaade.data.network.CollectionDto
import com.example.tamaade.data.network.ProductDetailDto
import com.example.tamaade.data.network.ProductListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("product/storefront/api/products/")
    suspend fun getProducts(
        @Header("Currency") currency: String = "GHS",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("name") name: String? = null,
        @Query("category") category: Int? = null,
        @Query("category_name") categoryName: String? = null,
        @Query("brand") brand: String? = null,
        @Query("collection") collection: Int? = null,
        @Query("ordering") ordering: String? = null,
        @Query("search") search: String? = null
    ): Response<ProductListResponse>

    @GET("product/storefront/api/products/{slug}/retrive_with_image_list/")
    suspend fun getProductDetails(
        @Path("slug") slug: String,
        @Header("Currency") currency: String = "GHS"
    ): Response<ProductDetailDto>

    @GET("product/storefront/api/recursive-categories/")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @GET("product/storefront/api/collections/")
    suspend fun getCollections(): Response<List<CollectionDto>>

    @GET("product/storefront/api/products/{slug}/with_recommended/")
    suspend fun getRecommendedProducts(
        @Path("slug") slug: String,
        @Header("Currency") currency: String = "GHS"
    ): Response<ProductListResponse>
}
