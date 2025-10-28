package com.example.tamaade.data.repository

import com.example.tamaade.api.RetrofitInstance
import com.example.tamaade.data.local.CartDao
import com.example.tamaade.data.local.CartItem
import com.example.tamaade.data.local.FavoriteDao
import com.example.tamaade.data.local.FavoriteItem
import com.example.tamaade.data.remote.model.Category
import com.example.tamaade.data.remote.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val cartDao: CartDao, private val favoriteDao: FavoriteDao) {

    suspend fun getProducts(): List<Product> {
        return RetrofitInstance.api.getProducts()
    }

    suspend fun getCategories(): List<Category> {
        return RetrofitInstance.api.getCategories()
    }

    fun getCartItems(): Flow<List<CartItem>> {
        return cartDao.getCartItems()
    }

    suspend fun addToCart(productId: Int, quantity: Int) {
        cartDao.insert(CartItem(productId, quantity))
    }

    suspend fun removeFromCart(productId: Int) {
        cartDao.delete(productId)
    }

    fun getFavoriteItems(): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoriteItems()
    }

    suspend fun isFavorite(productId: Int): Boolean {
        return favoriteDao.getFavoriteItem(productId) != null
    }

    suspend fun addToFavorites(productId: Int) {
        favoriteDao.insert(FavoriteItem(productId))
    }

    suspend fun removeFromFavorites(productId: Int) {
        favoriteDao.delete(productId)
    }
}
