package com.example.tamaade.data.remote.model

import com.google.gson.annotations.SerializedName

// This data class now perfectly matches the API response and your requirements.
data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("desc") val desc: String,
    @SerializedName("category") val category: String,
    @SerializedName("image") val image: String?,
    @SerializedName("video") val video: String?,
    @SerializedName("price") val price: String, // Correctly defined as String
    @SerializedName("quantity") val quantity: Int
)
