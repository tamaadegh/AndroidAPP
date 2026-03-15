package com.example.tamaade.data.network

import com.google.gson.annotations.SerializedName

data class ProductListResponse(
    val count: Int,
    @SerializedName("current_pagination_step")
    val pagination: PaginationStep?,
    val results: List<ProductListItemDto>
)

data class PaginationStep(
    @SerializedName("previous_url")
    val previousUrl: String?,
    @SerializedName("next_url")
    val nextUrl: String?
)

data class ProductListItemDto(
    val id: Int,
    val texts: TextsDto,
    val slug: String,
    @SerializedName("default_variant")
    val defaultVariant: VariantDto,
    @SerializedName("product_thumbnail")
    val productThumbnail: String
)

data class ProductDetailDto(
    val id: Int,
    val texts: TextsDto,
    val brand: String?,
    val category: Int,
    val collections: List<Int>,
    val variants: List<VariantDto>,
    val slug: String,
    val images: List<ImageDto>,
    @SerializedName("product_thumbnail")
    val productThumbnail: String
)

data class TextsDto(
    val name: String,
    val summary: String?,
    val description: String?
)

data class VariantDto(
    val id: Int,
    val alias: String,
    val name: String,
    val price: String
)

data class ImageDto(
    val id: Int,
    val image: String,
    @SerializedName("alt_text")
    val altText: String?
)

data class CategoryDto(
    val id: Int,
    val name: String,
    val description: String,
    val children: List<CategoryDto>
)

data class CollectionDto(
    val id: Int,
    val name: String,
    val description: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    val image: String?
)
