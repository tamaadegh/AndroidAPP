package com.example.tamaade.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: Int,
    @SerializedName("name")
    val productName: String,
    @SerializedName("desc")
    val productDescription: String,
    @SerializedName("image")
    val productImage: String?,
    @SerializedName("price")
    val productPrice: String,
    val quantity: Int,
    @SerializedName("category")
    val productCategory: String,
    val productBrand: String?,
    val productRating: Float = 0f,
    val productHave: Boolean? = null,
    val productDisCount: String? = null
) : Parcelable