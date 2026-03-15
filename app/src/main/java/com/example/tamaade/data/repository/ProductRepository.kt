package com.example.tamaade.data.repository

import com.example.tamaade.data.local.CartDao
import com.example.tamaade.data.local.CartItem
import com.example.tamaade.data.local.FavoriteDao
import com.example.tamaade.data.local.FavoriteItem
import com.example.tamaade.data.local.ProductDao
import com.example.tamaade.data.local.ProductEntity
import com.example.tamaade.data.remote.api.ApiService
import com.example.tamaade.data.remote.model.Product
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.tamaade.data.local.ProductDetailCacheEntity
import com.example.tamaade.data.remote.api.RetrofitClient
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class ProductRepository(
    private val apiService: ApiService,
    private val cartDao: CartDao? = null,
    private val favoriteDao: FavoriteDao? = null,
    private val productDao: ProductDao? = null,
    private val productDetailDao: com.example.tamaade.data.local.ProductDetailDao? = null
) {
    private val gson = Gson()
    private val CACHE_TIMEOUT = 5 * 60 * 1000L // 5 minutes

    // Constructor for ViewModelFactory
    constructor(cartDao: CartDao, favoriteDao: FavoriteDao, productDao: ProductDao) : this(
        RetrofitClient.instance,
        cartDao,
        favoriteDao,
        productDao
    )

    // Constructor for ViewModelFactory (kept for compatibility but updated to use productDao internally if available via other means or just null)
    // Actually, I should just update the factory usage.
    constructor(cartDao: CartDao, favoriteDao: FavoriteDao) : this(
        RetrofitClient.instance,
        cartDao,
        favoriteDao,
        null
    )

    // --- Product List Caching ---

    // Get products as a Flow from DB (Single Source of Truth)
    fun getProductsFlow(): Flow<List<Product>> {
        return productDao?.getAllProducts()?.map { entities ->
            entities.map { entity ->
                Product(
                    id = entity.id,
                    name = entity.name,
                    slug = entity.slug,
                    desc = entity.desc,
                    category = entity.category,
                    image = entity.image,
                    video = entity.video,
                    price = entity.price,
                    quantity = entity.quantity
                )
            }
        } ?: flowOf(emptyList())
    }

    // Refresh products from network and update DB
    suspend fun refreshProducts(currency: String = "GHS", limit: Int = 20, offset: Int = 0, search: String? = null) {
        try {
            val response = apiService.getProducts(currency, limit, offset, search = search)
            if (response.isSuccessful && response.body() != null) {
                val products = response.body()!!.results
                val entities = products.map { dto ->
                     ProductEntity(
                        id = dto.id,
                        name = dto.texts.name,
                        slug = dto.slug,
                        desc = dto.texts.description ?: dto.texts.summary ?: "",
                        category = "", // Category might be missing in DTO root
                        image = dto.productThumbnail,
                        video = null,
                        price = dto.defaultVariant.price,
                        quantity = 0
                    )
                }
                
                if (offset == 0 && search == null) {
                    // Start of list, maybe clear old cache if it's a full refresh?
                    // But for infinite scroll, we append.
                    // If it's a swipe-to-refresh (offset=0), we can clear or just upsert.
                    // If we clear, we lose existing data momentarily.
                    // Upsert handles updates. We might want to remove items no longer in server response?
                    // For simplicity and "everlasting scroll", upsert is safer.
                    // But if items are deleted on server, they remain in cache.
                    // Let's clear if offset is 0 to refresh the "head".
                    // But then we lose the rest of the scroll.
                    // Actually, SwipeRefresh usually signals a reset.
                    if (offset == 0) {
                         // Ideally we'd clear, but that clears ALL pages.
                         // Let's just upsert for now to be safe.
                         // Or create a timestamp field to prune old items.
                    }
                }
                productDao?.insertAll(entities)
            }
        } catch (e: Exception) {
            // Throw so ViewModel knows to show error, but existing cache remains valid
            throw e
        }
    }

    // Search specifically
    fun searchProductsFlow(query: String): Flow<List<Product>> {
        return productDao?.searchProducts(query)?.map { entities ->
            entities.map { entity -> 
                Product(
                    id = entity.id, 
                    name = entity.name, 
                    slug = entity.slug, 
                    desc = entity.desc, 
                    category = entity.category, 
                    image = entity.image, 
                    video = entity.video, 
                    price = entity.price, 
                    quantity = entity.quantity
                ) 
            }
        } ?: flowOf(emptyList())
    }
    
    // --- Favorites Caching ---

    fun getFavoriteProductsFlow(): Flow<List<Product>> {
        return productDao?.getFavoriteProducts()?.map { entities ->
            entities.map { entity ->
                Product(
                    id = entity.id,
                    name = entity.name,
                    slug = entity.slug,
                    desc = entity.desc,
                    category = entity.category,
                    image = entity.image,
                    video = entity.video,
                    price = entity.price,
                    quantity = entity.quantity
                )
            }
        } ?: flowOf(emptyList())
    }

    // --- Detailed Product Caching ---

    suspend fun getProductDetailCached(slug: String): com.example.tamaade.data.network.ProductDetailDto? {
        val now = System.currentTimeMillis()
        val cached = productDetailDao?.getProduct(slug)
        
        if (cached != null && (now - cached.timestamp) < CACHE_TIMEOUT) {
             return gson.fromJson(cached.dataJson, com.example.tamaade.data.network.ProductDetailDto::class.java)
        }

        try {
            val response = apiService.getProductDetails(slug)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                productDetailDao?.insert(ProductDetailCacheEntity(
                    slug = slug,
                    dataJson = gson.toJson(dto),
                    timestamp = now
                ))
                return dto
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return cached?.let { gson.fromJson(it.dataJson, com.example.tamaade.data.network.ProductDetailDto::class.java) }
    }

    // --- Other Methods ---

    suspend fun getProducts(
        currency: String = "GHS",
        limit: Int = 20,
        offset: Int = 0,
        search: String? = null
    ) = apiService.getProducts(currency, limit, offset, search = search)

    suspend fun getProductDetails(slug: String, currency: String = "GHS") = apiService.getProductDetails(slug, currency)
    suspend fun getCategories() = apiService.getCategories()
    suspend fun getCollections() = apiService.getCollections()
    suspend fun getRecommendedProducts(slug: String, currency: String = "GHS") = apiService.getRecommendedProducts(slug, currency)

    fun getFavoriteItems(): Flow<List<FavoriteItem>> = favoriteDao?.getFavoriteItems() 
        ?: flowOf(emptyList())

    suspend fun isFavorite(productId: Int): Boolean = favoriteDao?.getFavoriteItem(productId) != null

    suspend fun addToFavorites(productId: Int) {
        favoriteDao?.insert(FavoriteItem(productId))
    }

    suspend fun removeFromFavorites(productId: Int) {
        favoriteDao?.delete(productId)
    }

    suspend fun addToCart(productId: Int, quantity: Int) {
        cartDao?.insert(CartItem(productId, quantity))
    }
}
