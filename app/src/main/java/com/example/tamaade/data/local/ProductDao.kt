package com.example.tamaade.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :search || '%' OR desc LIKE '%' || :search || '%'")
    fun searchProducts(search: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("SELECT * FROM products WHERE id IN (SELECT productId FROM favorite_items)")
    fun getFavoriteProducts(): Flow<List<ProductEntity>>
}
