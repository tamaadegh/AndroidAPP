package com.tamaade.ecommerce.ui.deals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.CatalogData
import com.tamaade.ecommerce.data.productsForSection
import com.tamaade.ecommerce.ui.components.EmptyState
import com.tamaade.ecommerce.ui.components.ErrorState
import com.tamaade.ecommerce.ui.components.LoadingState
import com.tamaade.ecommerce.ui.components.ProductCard
import com.tamaade.ecommerce.ui.components.SectionHeader
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.PageBackground
import com.tamaade.ecommerce.ui.theme.White

/** Mirrors TamaadeWeb /deals: "deals_header" promo + home-sections with location=deals. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(
    catalog: CatalogData,
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddToCart: (Int) -> Unit,
    onOpenLink: (link: String?, default: String) -> Unit
) {
    val header = catalog.promo("deals_header")
    // Sections configured for the deals page; without any, list the products that are on offer.
    val sections = catalog.dealsSections
        .map { it to productsForSection(it.productSource, catalog.products) }
        .filter { it.second.isNotEmpty() }
    val fallbackDeals = if (sections.isEmpty()) catalog.deals else emptyList()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

            if (loading && catalog.products.isEmpty()) {
                item(span = fullSpan) { LoadingState() }
                return@LazyVerticalGrid
            }
            if (error != null && catalog.products.isEmpty()) {
                item(span = fullSpan) { ErrorState(message = error, onRetry = onRetry) }
                return@LazyVerticalGrid
            }

            item(span = fullSpan) {
                if (header != null) {
                    Surface(
                        color = BrandGreen,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenLink(header.link, "/products") }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = header.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = White,
                                textAlign = TextAlign.Center
                            )
                            if (header.subtitle.isNotBlank()) {
                                Text(
                                    text = header.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Text(
                                text = header.ctaLabel.ifBlank { "SHOP NOW" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen,
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(White)
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = "Deals",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen
                        )
                        Text(
                            text = "Current offers across the store",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            sections.forEach { (section, products) ->
                item(key = "deal_section_${section.id}", span = fullSpan) {
                    SectionHeader(
                        title = section.title,
                        subtitle = section.subtitle,
                        actionLabel = section.ctaLabel.ifBlank { "View all" },
                        onAction = { onOpenLink(section.link, "/products") }
                    )
                }
                items(products, key = { "deal_${section.id}_${it.id}" }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        onAddToCart = { onAddToCart(product.id) }
                    )
                }
            }

            items(fallbackDeals, key = { "deal_${it.id}" }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { onAddToCart(product.id) }
                )
            }

            if (sections.isEmpty() && fallbackDeals.isEmpty()) {
                item(span = fullSpan) {
                    EmptyState(
                        title = "No deals right now",
                        message = "New offers will appear here. Pull down to refresh."
                    )
                }
            }
        }
    }
}
