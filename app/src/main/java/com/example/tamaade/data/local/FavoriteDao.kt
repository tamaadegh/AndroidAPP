package com.example.tamaade.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_items")
    fun getFavoriteItems(): Flow<List<FavoriteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteItem: FavoriteItem)

    @Query("DELETE FROM favorite_items WHERE productId = :productId")
    suspend fun delete(productId: Int)

    @Query("SELECT * FROM favorite_items WHERE productId = :productId")
    suspend fun getFavoriteItem(productId: Int): FavoriteItem?
}
