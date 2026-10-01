package com.tamaade.ecommerce.ui.navigation

import android.net.Uri
import com.tamaade.ecommerce.data.ProductSort
import com.tamaade.ecommerce.data.model.ProductCategory

/** Where an admin-configured link (banner, tile, promo, section "view all") should take the user. */
sealed interface StoreLink {
    data object Home : StoreLink
    data object Deals : StoreLink
    data object Categories : StoreLink
    data object Cart : StoreLink
    data object Profile : StoreLink
    data object Privacy : StoreLink
    data class ProductList(
        val category: String = "",
        val maxPrice: Int = -1,
        val query: String = "",
        val sort: String = ProductSort.Default
    ) : StoreLink
    data class Product(val id: Int) : StoreLink
    data class External(val url: String) : StoreLink
}

/**
 * Maps TamaadeWeb-style paths ("/deals", "/products?category=Shoes", "/products/12",
 * "/category/3", "/allcategories", ...) to app screens. Absolute URLs open in the browser.
 */
fun resolveStoreLink(
    link: String?,
    categories: List<ProductCategory>,
    fallback: StoreLink = StoreLink.ProductList()
): StoreLink {
    val raw = link?.trim().orEmpty()
    if (raw.isEmpty()) return fallback
    if (raw.startsWith("http://") || raw.startsWith("https://")) return StoreLink.External(raw)

    val uri = Uri.parse(if (raw.startsWith("/")) raw else "/$raw")
    val segments = uri.pathSegments.orEmpty()
    fun param(name: String): String = uri.getQueryParameter(name).orEmpty()

    return when (segments.firstOrNull()) {
        null, "" -> StoreLink.Home
        "deals" -> StoreLink.Deals
        "allcategories", "categories" -> StoreLink.Categories
        "cart", "checkout" -> StoreLink.Cart
        "profile", "login", "register" -> StoreLink.Profile
        "privacy" -> StoreLink.Privacy
        "category" -> {
            val id = segments.getOrNull(1)?.toIntOrNull()
            val name = categories.find { it.id == id }?.name
            if (name != null) StoreLink.ProductList(category = name) else StoreLink.Categories
        }
        "products" -> {
            val id = segments.getOrNull(1)?.toIntOrNull()
            if (id != null) {
                StoreLink.Product(id)
            } else {
                val ordering = param("ordering")
                val filter = param("filter")
                StoreLink.ProductList(
                    category = param("category"),
                    maxPrice = param("maxPrice").toDoubleOrNull()?.toInt() ?: -1,
                    query = param("search").ifBlank { param("q") },
                    sort = when {
                        ordering == "-created_at" || filter == "back-to-stock" -> ProductSort.Newest
                        filter == "top-selling" || filter == "top-picks" -> ProductSort.Top
                        else -> ProductSort.Default
                    }
                )
            }
        }
        else -> fallback
    }
}
