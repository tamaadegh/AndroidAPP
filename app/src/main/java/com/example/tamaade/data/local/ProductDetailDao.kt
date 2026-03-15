package com.example.tamaade.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDetailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: ProductDetailCacheEntity)

    @Query("SELECT * FROM product_detail_cache WHERE slug = :slug")
    suspend fun getProduct(slug: String): ProductDetailCacheEntity?

    @Query("DELETE FROM product_detail_cache WHERE slug = :slug")
    suspend fun delete(slug: String)
    
    @Query("DELETE FROM product_detail_cache WHERE timestamp < :expirationTime")
    suspend fun clearExpired(expirationTime: Long)
}
