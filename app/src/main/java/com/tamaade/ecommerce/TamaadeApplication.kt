package com.tamaade.ecommerce

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.tamaade.ecommerce.data.api.TamaadeApi

class TamaadeApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // filesDir (not cacheDir) so Android doesn't wipe the saved catalog under storage pressure.
        TamaadeApi.initResponseCache(filesDir.resolve("catalog_cache"))
    }

    /** One shared Coil loader: big disk cache so product photos survive app restarts. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            // Cloudinary URLs never change content, so skip HTTP revalidation round-trips.
            .respectCacheHeaders(false)
            .crossfade(150)
            .build()
}
