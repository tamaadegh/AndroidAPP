package com.example.tamaade.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_detail_cache")
data class ProductDetailCacheEntity(
    @PrimaryKey
    val slug: String,
    val dataJson: String,
    val timestamp: Long
)
