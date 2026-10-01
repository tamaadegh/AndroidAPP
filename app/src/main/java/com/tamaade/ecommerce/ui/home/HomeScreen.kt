package com.tamaade.ecommerce.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.CatalogData
import com.tamaade.ecommerce.data.model.HeroBanner
import com.tamaade.ecommerce.data.model.MerchTile
import com.tamaade.ecommerce.data.model.SiteConfig
import com.tamaade.ecommerce.data.productsForSection
import com.tamaade.ecommerce.ui.components.CategoryChip
import com.tamaade.ecommerce.ui.components.EmptyState
import com.tamaade.ecommerce.ui.components.ErrorState
import com.tamaade.ecommerce.ui.components.LoadingState
import com.tamaade.ecommerce.ui.components.ProductCard
import com.tamaade.ecommerce.ui.components.ProductImage
import com.tamaade.ecommerce.ui.components.PromoBanner
import com.tamaade.ecommerce.ui.components.SectionHeader
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.PageBackground
import com.tamaade.ecommerce.ui.util.formatPrice
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalog: CatalogData,
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddToCart: (Int) -> Unit,
    onOpenLink: (link: String?, default: String) -> Unit,
    onOpenMaxPrice: (Int) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            val nothingLoaded = catalog.products.isEmpty() && catalog.banners.isEmpty()
            when {
                loading && nothingLoaded -> {
                    item { LoadingState() }
                    return@LazyColumn
                }
                error != null && nothingLoaded -> {
                    item { ErrorState(message = error, onRetry = onRetry) }
                    return@LazyColumn
                }
            }

            catalog.promo("top_strip")?.let { promo ->
                item(key = "top_strip") {
                    PromoBanner(
                        title = promo.title,
                        highlight = promo.highlight,
                        ctaLabel = promo.ctaLabel,
                        onClick = { onOpenLink(promo.link, "/deals") }
                    )
                }
            }

            if (catalog.banners.isNotEmpty()) {
                item(key = "hero") {
                    HeroCarousel(
                        banners = catalog.banners,
                        onClick = { onOpenLink(it.link, "/deals") }
                    )
                }
            }

            if (catalog.priceTiers.isNotEmpty()) {
                item(key = "price_tiers") {
                    SectionHeader(
                        title = "Shop by budget",
                        subtitle = "Great finds under every price",
                        onAction = null
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(catalog.priceTiers, key = { it.id }) { tier ->
                            Surface(
                                onClick = { onOpenMaxPrice(tier.amount.toInt()) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.width(100.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Under", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = formatPrice(tier.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            merchRow("main_tiles", catalog.mainTiles, onOpenLink)
            merchRow("featured_tiles", catalog.featuredTiles, onOpenLink)

            catalog.homeSections.forEach { section ->
                val products = productsForSection(section.productSource, catalog.products)
                if (products.isEmpty()) return@forEach
                item(key = "section_${section.id}") {
                    SectionHeader(
                        title = section.title,
                        subtitle = section.subtitle,
                        actionLabel = section.ctaLabel.ifBlank { "View all" },
                        onAction = { onOpenLink(section.link, "/products") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProductClick(product.id) },
                                onAddToCart = { onAddToCart(product.id) },
                                modifier = Modifier.width(168.dp)
                            )
                        }
                    }
                }
            }

            merchRow("bottom_tiles", catalog.bottomTiles, onOpenLink)

            val hasContent = catalog.products.isNotEmpty() || catalog.banners.isNotEmpty() ||
                catalog.mainTiles.isNotEmpty() || catalog.featuredTiles.isNotEmpty()
            if (!hasContent) {
                item(key = "empty") {
                    EmptyState(
                        title = "The store is getting ready",
                        message = "There are no products yet. Pull down to refresh or check back soon.",
                        actionLabel = "Refresh",
                        onAction = onRefresh
                    )
                }
            }
        }
    }
}

private fun LazyListScope.merchRow(
    key: String,
    tiles: List<MerchTile>,
    onOpenLink: (link: String?, default: String) -> Unit
) {
    if (tiles.isEmpty()) return
    item(key = key) {
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tiles, key = { it.id }) { tile ->
                CategoryChip(
                    title = tile.title,
                    imageUrl = tile.image,
                    badge = tile.badge,
                    highlight = tile.highlight,
                    onClick = { onOpenLink(tile.link, "/products") }
                )
            }
        }
    }
}

@Composable
private fun HeroCarousel(banners: List<HeroBanner>, onClick: (HeroBanner) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    if (banners.size > 1) {
        LaunchedEffect(pagerState, banners.size) {
            while (true) {
                delay(5_000)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
            }
        }
    }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val banner = banners[page]
            ProductImage(
                model = banner.image,
                contentDescription = banner.title.ifBlank { SiteConfig.name },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClick(banner) }
            )
        }
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                banners.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
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
