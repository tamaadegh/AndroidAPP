package com.example.tamaade.data.repository

import com.example.tamaade.data.local.CartDao
import com.example.tamaade.data.local.FavoriteDao
import com.example.tamaade.data.network.RetrofitClient
import com.example.tamaade.data.model.Product
import com.example.tamaade.data.network.ProductDto

class ProductRepository(private val cartDao: CartDao, private val favoriteDao: FavoriteDao) {

    private val apiService = RetrofitClient.instance

    suspend fun getProducts(): List<Product> {
        return apiService.getProducts().results.map { it.toProduct() }
    }

    suspend fun getProductDetail(slug: String): Product {
        return apiService.getProductDetail(slug).toProduct()
    }
}

fun ProductDto.toProduct(): Product {
    return Product(
        id = id,
        productName = name,
        productImage = productThumbnail,
        productPrice = "", // You may want to add price to your Dto
        productDescription = "", // You may want to add description to your Dto
    )
}