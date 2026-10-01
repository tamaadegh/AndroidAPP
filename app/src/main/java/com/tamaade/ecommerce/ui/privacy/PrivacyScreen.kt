package com.tamaade.ecommerce.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.api.ApiException
import com.tamaade.ecommerce.data.api.TamaadeApi
import com.tamaade.ecommerce.data.model.PrivacyContent
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Muted
import com.tamaade.ecommerce.ui.theme.PageBackground

/** Shown when the backend has no policy yet or can't be reached. */
private val FallbackPrivacy = PrivacyContent(
    title = "Privacy Policy",
    content = """
        ## What we collect
        Tamaade only collects your data when you shop with us. We use it to run your basket, orders, payment and delivery — nothing else.

        - Cart: the products and quantities you add to your basket.
        - Orders: what you bought, when, and the order status.
        - Checkout and payment: payment references and status from Hubtel. Your card or mobile money details are entered on Hubtel's secure page and are never stored by Tamaade.
        - Delivery details: your name, phone number and address so we can deliver your order.
        - Account: your name and email address when you create an account.

        ## How we use it
        We use this information only to process your orders, take payment, deliver your items and support you. We do not sell your personal data.

        ## Your choices
        You can browse Tamaade without an account. You can sign out at any time, and you can contact us to update or delete your account data.
    """.trimIndent(),
    updatedAt = null
)

private sealed interface PolicyBlock {
    data class Heading(val text: String) : PolicyBlock
    data class Paragraph(val text: String) : PolicyBlock
    data class Bullet(val text: String) : PolicyBlock
}

/**
 * Admin-edited plain text: blank lines separate blocks, "## " lines are headings,
 * "- " lines are bullets, everything else is paragraph text.
 */
private fun parsePolicy(content: String): List<PolicyBlock> {
    val blocks = mutableListOf<PolicyBlock>()
    content.replace("\r\n", "\n")
        .split(Regex("\\n\\s*\\n"))
        .forEach { chunk ->
            val paragraph = StringBuilder()
            fun flush() {
                if (paragraph.isNotBlank()) blocks += PolicyBlock.Paragraph(paragraph.toString().trim())
                paragraph.clear()
            }
            chunk.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                when {
                    line.startsWith("## ") -> {
                        flush()
                        blocks += PolicyBlock.Heading(line.removePrefix("## ").trim())
                    }
                    line.startsWith("- ") -> {
                        flush()
                        blocks += PolicyBlock.Bullet(line.removePrefix("- ").trim())
                    }
                    else -> {
                        if (paragraph.isNotEmpty()) paragraph.append(' ')
                        paragraph.append(line)
                    }
                }
            }
            flush()
        }
    return blocks
}

private data class PolicyLoad(val loading: Boolean, val policy: PrivacyContent)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val state by produceState(PolicyLoad(loading = true, policy = FallbackPrivacy)) {
        value = try {
            val remote = TamaadeApi.privacy()
            PolicyLoad(false, if (remote.content.isBlank()) FallbackPrivacy else remote)
        } catch (_: ApiException) {
            PolicyLoad(false, FallbackPrivacy)
        }
    }
    val blocks = parsePolicy(state.policy.content)

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
            Text(
                text = state.policy.title.ifBlank { "Privacy Policy" },
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.policy.updatedAt?.let { updated ->
                    item {
                        Text(
                            text = "Last updated ${updated.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                }
                items(blocks) { block ->
                    when (block) {
                        is PolicyBlock.Heading -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        is PolicyBlock.Paragraph -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Foreground
                        )
                        is PolicyBlock.Bullet -> Row(modifier = Modifier.padding(start = 4.dp)) {
                            Text("•  ", style = MaterialTheme.typography.bodyMedium, color = BrandGreen)
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Foreground
                            )
                        }
                    }
                }
            }
        }
    }
}
