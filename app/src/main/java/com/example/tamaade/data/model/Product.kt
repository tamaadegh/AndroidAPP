package com.example.tamaade.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Product data model
@Parcelize
data class Product(
    val id: Int,
    val productName: String,
    val productCategory: String,
    val productDescription: String,
    val productImage: String?,
    val productPrice: String,
    val slug: String
) : Parcelable