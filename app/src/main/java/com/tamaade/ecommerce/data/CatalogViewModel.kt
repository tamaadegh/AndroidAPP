package com.tamaade.ecommerce.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamaade.ecommerce.data.api.ApiException
import com.tamaade.ecommerce.data.api.TamaadeApi
import com.tamaade.ecommerce.data.model.HeroBanner
import com.tamaade.ecommerce.data.model.HomeSection
import com.tamaade.ecommerce.data.model.MerchTile
import com.tamaade.ecommerce.data.model.PriceTier
import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.data.model.ProductCategory
import com.tamaade.ecommerce.data.model.StorefrontPromo
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Everything the storefront shows, straight from the public catalog endpoints. */
data class CatalogData(
    val products: List<Product> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val banners: List<HeroBanner> = emptyList(),
    val priceTiers: List<PriceTier> = emptyList(),
    val promos: List<StorefrontPromo> = emptyList(),
    val mainTiles: List<MerchTile> = emptyList(),
    val featuredTiles: List<MerchTile> = emptyList(),
    val bottomTiles: List<MerchTile> = emptyList(),
    val homeSections: List<HomeSection> = emptyList(),
    val dealsSections: List<HomeSection> = emptyList()
) {
    fun promo(key: String): StorefrontPromo? = promos.find { it.key == key }

    fun productById(id: Int): Product? = products.find { it.id == id }

    /** Discounted / promo products, used on Deals when the backend has no deals sections. */
    val deals: List<Product>
        get() = products.filter { it.isDeal }
}

/** Sort keys carried by product-list links (mirrors TamaadeWeb /products?ordering=… / ?filter=…). */
object ProductSort {
    const val Default = ""
    const val Newest = "newest"
    const val Top = "top"
}

class CatalogViewModel : ViewModel() {
    var data by mutableStateOf(CatalogData())
        private set

    /** First load (or retry after an error) with nothing on screen yet. */
    var loading by mutableStateOf(true)
        private set

    /** Pull-to-refresh in progress. */
    var refreshing by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var loadedOnce by mutableStateOf(false)
        private set

    /** Bumped after every successful products load so the basket can be re-synced. */
    var productsVersion by mutableStateOf(0)
        private set

    private var loadJob: Job? = null

    init {
        load(pull = false)
    }

    fun refresh() = load(pull = true)

    fun retry() = load(pull = false)

    private fun load(pull: Boolean) {
        if (loadJob?.isActive == true) return
        if (pull) refreshing = true else loading = true
        error = null
        loadJob = viewModelScope.launch {
            try {
                data = coroutineScope {
                    val products = async { TamaadeApi.products() }
                    val categories = async { optional { TamaadeApi.categories() } }
                    val banners = async { optional { TamaadeApi.banners() } }
                    val tiers = async { optional { TamaadeApi.priceTiers() } }
                    val promos = async { optional { TamaadeApi.promos() } }
                    val main = async { optional { TamaadeApi.merchTiles("main") } }
                    val featured = async { optional { TamaadeApi.merchTiles("featured") } }
                    val bottom = async { optional { TamaadeApi.merchTiles("bottom") } }
                    val home = async { optional { TamaadeApi.homeSections("home") } }
                    val deals = async { optional { TamaadeApi.homeSections("deals") } }
                    CatalogData(
                        products = products.await(),
                        categories = categories.await(),
                        banners = banners.await(),
                        priceTiers = tiers.await(),
                        promos = promos.await(),
                        mainTiles = main.await(),
                        featuredTiles = featured.await(),
                        bottomTiles = bottom.await(),
                        homeSections = home.await(),
                        dealsSections = deals.await()
                    )
                }
                loadedOnce = true
                productsVersion++
            } catch (e: ApiException) {
                error = e.message ?: "Couldn't load the store. Please try again."
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    /** Product from the loaded list, else GET api/products/<id>/. */
    suspend fun findProduct(id: Int): Product? =
        data.productById(id) ?: try {
            TamaadeApi.product(id)
        } catch (_: ApiException) {
            null
        }

    /** Secondary storefront content is optional: a failing endpoint just hides its section. */
    private suspend fun <T> optional(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (_: ApiException) {
        emptyList()
    }
}

/** Same rules as TamaadeWeb productsForSection(). */
fun productsForSection(source: String, products: List<Product>): List<Product> = when (source) {
    "newest" -> sortNewest(products).take(12)
    // No sales data in the API yet: show the catalog order rather than a fake ranking.
    "bestsellers" -> products.take(12)
    else -> products.take(12)
}

fun sortNewest(products: List<Product>): List<Product> =
    products.sortedByDescending { it.createdAt.orEmpty() }

/** Client-side filtering, matching TamaadeWeb /products. */
fun filterProducts(
    products: List<Product>,
    category: String = "",
    maxPrice: Int = -1,
    query: String = "",
    sort: String = ProductSort.Default
): List<Product> {
    var result = products
    if (query.isNotBlank()) {
        val q = query.trim().lowercase()
        result = result.filter {
            it.name.lowercase().contains(q) ||
                it.desc.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.brand.lowercase().contains(q)
        }
    }
    if (category.isNotBlank()) {
        val cat = category.lowercase()
        result = result.filter {
            val pc = it.category.lowercase()
            pc.isNotEmpty() && (pc.contains(cat) || cat.contains(pc))
        }
    }
    if (maxPrice > 0) result = result.filter { it.priceValue <= maxPrice }
    return when (sort) {
        ProductSort.Newest -> sortNewest(result)
        ProductSort.Top -> result
        else -> result
    }
}
