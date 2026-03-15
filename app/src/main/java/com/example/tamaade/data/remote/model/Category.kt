package com.example.tamaade.data.remote.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("icon") val icon: String?
)
