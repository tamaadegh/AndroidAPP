package com.example.tamaade.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val slug: String,
    val desc: String,
    val category: String,
    val image: String?,
    val video: String?,
    val price: String,
    val quantity: Int
)
