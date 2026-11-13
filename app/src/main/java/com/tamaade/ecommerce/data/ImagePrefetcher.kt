package com.tamaade.ecommerce.data

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.tamaade.ecommerce.data.model.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Downloads images before the screens that show them, using the same URLs as ProductImage
 * so the requests hit Coil's caches.
 */
object ImagePrefetcher {

    /** How many products of each home section are on (or right below) the first screen. */
    private const val FIRST_SCREEN_PRODUCTS = 6

    /**
     * Decodes what the home screen shows first (banners, tiles, the first products) into the
     * memory cache and returns once they are ready, so Home opens with its images in place.
     */
    suspend fun warmUpHome(context: Context, data: CatalogData) {
        val urls = buildList {
            data.banners.forEach { add(optimizedImageUrl(it.image, ImageSize.Full)) }
            (data.mainTiles + data.featuredTiles).forEach { add(optimizedImageUrl(it.image, ImageSize.Thumb)) }
            data.homeSections.take(2).forEach { section ->
                productsForSection(section.productSource, data.products)
                    .take(FIRST_SCREEN_PRODUCTS)
                    .forEach { add(optimizedImageUrl(it.image, ImageSize.Card)) }
            }
        }.filterNotNull().distinct()

        val loader = context.imageLoader
        coroutineScope {
            urls.map { url ->
                async { loader.execute(ImageRequest.Builder(context).data(url).build()) }
            }.awaitAll()
        }
    }

    /**
     * Puts the rest of the catalog's images in the disk cache (not in memory, to keep RAM low),
     * so scrolling and other tabs show images without waiting on the network.
     */
    fun prefetchCatalog(context: Context, data: CatalogData) {
        val urls = buildList {
            data.products.forEach { add(optimizedImageUrl(it.image, ImageSize.Card)) }
            data.banners.forEach { add(optimizedImageUrl(it.image, ImageSize.Full)) }
            (data.mainTiles + data.featuredTiles + data.bottomTiles)
                .forEach { add(optimizedImageUrl(it.image, ImageSize.Thumb)) }
            data.categories.forEach { add(optimizedImageUrl(it.icon, ImageSize.Thumb)) }
        }.filterNotNull().distinct()
        urls.forEach { diskOnly(context, it) }
    }

    /** Every gallery photo of a product at detail size, so swiping the gallery is instant. */
    fun prefetchProductGallery(context: Context, product: Product) {
        galleryUrls(product)
            .mapNotNull { optimizedImageUrl(it, ImageSize.Full) }
            .forEach { diskOnly(context, it) }
    }

    fun galleryUrls(product: Product): List<String> =
        product.images.ifEmpty { listOfNotNull(product.image) }

    private fun diskOnly(context: Context, url: String) {
        context.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        )
    }
}
