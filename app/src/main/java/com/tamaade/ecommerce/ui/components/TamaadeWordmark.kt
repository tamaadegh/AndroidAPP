package com.tamaade.ecommerce.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.tamaade.ecommerce.R
import com.tamaade.ecommerce.data.ImageSize
import com.tamaade.ecommerce.data.optimizedImageUrl
import com.tamaade.ecommerce.data.model.SiteConfig
import com.tamaade.ecommerce.ui.theme.BrandGreen

/**
 * The real TAMAADE logo (drawable/tamaade_logo.png, trimmed to the wordmark).
 * Its background is BrandGreen (#365944), so it blends into green surfaces.
 */
@Composable
fun TamaadeWordmark(
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    Image(
        painter = painterResource(R.drawable.tamaade_logo),
        contentDescription = SiteConfig.name,
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height)
    )
}

/** Green tile with the logo, used while a product image loads or when it fails / is missing. */
@Composable
fun LogoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(BrandGreen),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.tamaade_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

/**
 * Remote product/category image that falls back to the Tamaade logo instead of a blank box.
 *
 * [size] picks the resized Cloudinary variant (see optimizedImageUrl). When [previewSize] is set,
 * that smaller variant is shown straight from cache while the [size] one downloads, e.g. the
 * card photo on the product page while the full-size photo arrives.
 */
@Composable
fun ProductImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    size: ImageSize = ImageSize.Card,
    previewSize: ImageSize? = null
) {
    val context = LocalContext.current
    val url = remember(model, size) { optimizedImageUrl(model, size) }
    val previewUrl = remember(model, previewSize) { previewSize?.let { optimizedImageUrl(model, it) } }

    val painter = rememberAsyncImagePainter(
        model = remember(url) {
            ImageRequest.Builder(context)
                .data(url)
                // A bigger cached bitmap (e.g. from the prefetch) is fine; don't decode again.
                .precision(Precision.INEXACT)
                .build()
        }
    )
    val previewPainter = rememberAsyncImagePainter(
        model = remember(previewUrl) {
            ImageRequest.Builder(context)
                .data(previewUrl)
                .precision(Precision.INEXACT)
                // Preview only if already cached: never download the same photo twice.
                .networkCachePolicy(CachePolicy.DISABLED)
                .crossfade(false)
                .build()
        }
    )

    val mainReady = painter.state is AsyncImagePainter.State.Success
    val previewReady = previewUrl != null && previewPainter.state is AsyncImagePainter.State.Success

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!mainReady && previewReady) {
            Image(
                painter = previewPainter,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
        if (!mainReady && !previewReady) {
            LogoPlaceholder(Modifier.fillMaxSize())
        }
    }
}
