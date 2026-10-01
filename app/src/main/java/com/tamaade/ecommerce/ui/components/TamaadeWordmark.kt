package com.tamaade.ecommerce.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.tamaade.ecommerce.R
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

/** Remote product/category image that falls back to the Tamaade logo instead of a blank box. */
@Composable
fun ProductImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val painter = rememberAsyncImagePainter(model = model)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
        if (painter.state !is AsyncImagePainter.State.Success) {
            LogoPlaceholder(Modifier.fillMaxSize())
        }
    }
}
