package com.example.tamaade.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val productName: String,
    val productDescription: String,
    val productImage: String?,
    val productPrice: String
) : Parcelable