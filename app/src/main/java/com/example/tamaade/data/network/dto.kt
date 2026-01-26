package com.example.tamaade.data.network

import com.google.gson.annotations.SerializedName

data class ProductListResponse(
    val count: Int,
    val results: List<ProductDto>
)

data class ProductDto(
    val id: Int,
    val name: String,
    val slug: String,
    @SerializedName("product_thumbnail")
    val productThumbnail: String,
    val images: List<ImageDto>
)

data class ImageDto(
    val id: Int,
    val image: String
)