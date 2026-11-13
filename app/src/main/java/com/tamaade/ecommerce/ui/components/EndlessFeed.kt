package com.tamaade.ecommerce.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.ui.theme.BrandGreen
import kotlin.random.Random

/** One card in an endless feed; [key] stays unique when a product comes back in a later round. */
data class FeedEntry(val key: String, val product: Product)

/**
 * Doomscroll-style product feed: reveals [pageSize] products at a time as the user scrolls.
 * When [loop] is on and the catalog runs out, it starts a new reshuffled round so the feed
 * never ends. Without [loop] it stops after the last product (used for searches and filters).
 */
class EndlessFeed(
    private val source: List<Product>,
    private val pageSize: Int,
    private val loop: Boolean,
    private val keyPrefix: String
) {
    var entries by mutableStateOf<List<FeedEntry>>(emptyList())
        private set

    /** Round 0 keeps the caller's order (sorting, sections); later rounds are shuffled. */
    private var round = 0
    private var order: List<Product> = source
    private var position = 0

    /** True while scrolling can still bring more cards (always, for a looping feed). */
    val hasMore: Boolean
        get() = source.isNotEmpty() && (position < order.size || canLoop)

    // Looping a handful of products would just show obvious repeats.
    private val canLoop: Boolean
        get() = loop && source.size >= MIN_PRODUCTS_TO_LOOP

    init {
        loadMore()
    }

    fun loadMore() {
        if (!hasMore) return
        val added = ArrayList<FeedEntry>(pageSize)
        while (added.size < pageSize) {
            if (position >= order.size) {
                if (!canLoop) break
                round++
                order = source.shuffled(Random(round * 7919 + source.size))
                position = 0
            }
            val product = order[position++]
            added += FeedEntry("${keyPrefix}_${round}_${product.id}", product)
        }
        if (added.isNotEmpty()) entries = entries + added
    }

    companion object {
        const val MIN_PRODUCTS_TO_LOOP = 8
        /** Start loading the next page when the user is this many items from the end. */
        const val PREFETCH_DISTANCE = 8
    }
}

@Composable
fun rememberEndlessFeed(
    source: List<Product>,
    pageSize: Int = 20,
    loop: Boolean = true,
    keyPrefix: String = "feed"
): EndlessFeed = remember(source, pageSize, loop, keyPrefix) {
    EndlessFeed(source, pageSize, loop, keyPrefix)
}

/** Loads the next page whenever the list gets close to its last item. */
@Composable
fun EndlessFeed.LoadMoreOnScroll(state: LazyListState) {
    LaunchedEffect(this, state) {
        snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Emit the total too, so a fresh page that is still too short triggers another load.
            (last >= info.totalItemsCount - EndlessFeed.PREFETCH_DISTANCE) to info.totalItemsCount
        }.collect { (nearEnd, _) -> if (nearEnd) loadMore() }
    }
}

@Composable
fun EndlessFeed.LoadMoreOnScroll(state: LazyGridState) {
    LaunchedEffect(this, state) {
        snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            (last >= info.totalItemsCount - EndlessFeed.PREFETCH_DISTANCE) to info.totalItemsCount
        }.collect { (nearEnd, _) -> if (nearEnd) loadMore() }
    }
}

/** Small spinner at the bottom of a feed that still has more to show. */
@Composable
fun FeedLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = BrandGreen,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
        )
    }
}
