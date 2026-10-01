package com.tamaade.ecommerce.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.ui.components.AuthTextField
import com.tamaade.ecommerce.ui.components.EmptyState
import com.tamaade.ecommerce.ui.components.ErrorState
import com.tamaade.ecommerce.ui.components.LoadingState
import com.tamaade.ecommerce.ui.components.ProductCard
import com.tamaade.ecommerce.ui.components.ProductImage
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.CardImageBg
import com.tamaade.ecommerce.ui.theme.PageBackground
import com.tamaade.ecommerce.ui.theme.PromoStripMid
import com.tamaade.ecommerce.ui.theme.PromoStripStart
import com.tamaade.ecommerce.ui.theme.StrikeGray
import com.tamaade.ecommerce.ui.util.formatPrice
import kotlinx.coroutines.launch

@Composable
fun ProductListScreen(
    title: String,
    products: List<Product>,
    query: String,
    onQueryChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddToCart: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandGreen)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${products.size} items",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        AuthTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search products",
            leadingIcon = Icons.Outlined.Search,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (products.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    when {
                        loading -> LoadingState()
                        error != null -> ErrorState(message = error, onRetry = onRetry)
                        else -> EmptyState(
                            title = "No products found",
                            message = "Try another search or category."
                        )
                    }
                }
            }
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { onAddToCart(product.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    loading: Boolean,
    onBack: () -> Unit,
    onAddToCart: (Product) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBackground),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                LoadingState()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        title = "Product not found",
                        message = "This product may have been removed.",
                        actionLabel = "Go back",
                        onAction = onBack
                    )
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(product.name, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandGreen,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatPrice(product.price), fontWeight = FontWeight.Bold)
                        product.originalPrice?.let {
                            Text(
                                formatPrice(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = StrikeGray,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onAddToCart(product)
                            scope.launch { snackbar.showSnackbar("Added to basket") }
                        },
                        enabled = product.quantity > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (product.quantity > 0) "Add to basket" else "Sold out")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(PageBackground)
            ) {
                ProductGallery(product)
                if (product.promoLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PromoStripStart, PromoStripMid, PromoStripStart)
                                )
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(product.promoLabel, color = BrandGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    if (product.brand.isNotBlank()) {
                        Text(
                            product.brand.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            formatPrice(product.price),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        product.effectiveDiscount?.let {
                            Text("-$it%", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (product.isExpress) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "express delivery",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .background(BrandGreen, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(product.desc, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    if (product.category.isNotBlank()) {
                        Text(
                            "Category · ${product.category}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (product.seller.isNotBlank()) {
                        Text(
                            "Sold by ${product.seller}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** Product images from the API (images[] then image); the logo shows when there are none. */
@Composable
private fun ProductGallery(product: Product) {
    val urls = product.images.ifEmpty { listOfNotNull(product.image) }
    val pagerState = rememberPagerState(pageCount = { urls.size.coerceAtLeast(1) })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(CardImageBg)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ProductImage(
                model = urls.getOrNull(page),
                contentDescription = product.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        }
        if (urls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                urls.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pagerState.currentPage) BrandGreen
                                else BrandGreen.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}
