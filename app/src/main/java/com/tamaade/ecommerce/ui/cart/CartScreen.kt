package com.tamaade.ecommerce.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.model.CartLine
import com.tamaade.ecommerce.data.model.CheckoutState
import com.tamaade.ecommerce.ui.components.ProductImage
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Muted
import com.tamaade.ecommerce.ui.theme.PageBackground
import com.tamaade.ecommerce.ui.theme.PromoStripStart
import com.tamaade.ecommerce.ui.theme.White
import com.tamaade.ecommerce.ui.util.formatPrice

@Composable
fun CartScreen(
    lines: List<CartLine>,
    total: Double,
    checkoutState: CheckoutState,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCheckout: () -> Unit,
    onCheckPayment: () -> Unit,
    onDismissStatus: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onContinueShopping: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        if (checkoutState is CheckoutState.Paid && lines.isEmpty()) {
            PaymentSuccess(
                orderId = checkoutState.orderId,
                onContinueShopping = {
                    onDismissStatus()
                    onContinueShopping()
                }
            )
        } else if (lines.isEmpty()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                CheckoutStatusBanner(checkoutState, onCheckPayment, onDismissStatus)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Your basket is empty",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Browse deals and add items to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onContinueShopping,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Continue shopping")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "checkout_status") {
                    CheckoutStatusBanner(checkoutState, onCheckPayment, onDismissStatus)
                }
                items(lines, key = { it.product.id }) { line ->
                    CartLineRow(
                        line = line,
                        onIncrement = { onIncrement(line.product.id) },
                        onDecrement = { onDecrement(line.product.id) },
                        onRemove = { onRemove(line.product.id) }
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatPrice(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    val busy = checkoutState is CheckoutState.Starting ||
                        checkoutState is CheckoutState.Verifying
                    Button(
                        onClick = onCheckout,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            disabledContainerColor = BrandGreen.copy(alpha = 0.6f),
                            disabledContentColor = White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                color = White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = when (checkoutState) {
                                CheckoutState.Starting -> "Opening Hubtel…"
                                is CheckoutState.Verifying -> "Checking payment…"
                                else -> "Checkout"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Secure payment with Hubtel ·",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                        TextButton(onClick = onOpenPrivacy) {
                            Text("Privacy Policy", style = MaterialTheme.typography.bodySmall, color = BrandGreen)
                        }
                    }
                }
            }
        }
    }
}

/** Shows what happened with the last Hubtel attempt (errors, pending, verifying). */
@Composable
private fun CheckoutStatusBanner(
    state: CheckoutState,
    onCheckPayment: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        CheckoutState.Idle, CheckoutState.Starting, is CheckoutState.Paid -> Unit
        is CheckoutState.AwaitingPayment -> StatusCard(
            icon = Icons.Outlined.HourglassTop,
            tint = BrandGreen,
            container = PromoStripStart,
            message = "Complete your payment on the Hubtel page, then come back to Tamaade.",
            actionLabel = "I've paid — check",
            onAction = onCheckPayment,
            onDismiss = onDismiss
        )
        is CheckoutState.Verifying -> StatusCard(
            icon = null,
            tint = BrandGreen,
            container = PromoStripStart,
            message = "Checking your payment with Hubtel…",
            loading = true
        )
        is CheckoutState.Pending -> StatusCard(
            icon = Icons.Outlined.HourglassTop,
            tint = BrandGreen,
            container = PromoStripStart,
            message = state.message,
            actionLabel = "Check again",
            onAction = onCheckPayment,
            onDismiss = onDismiss
        )
        is CheckoutState.Failed -> StatusCard(
            icon = Icons.Outlined.ErrorOutline,
            tint = MaterialTheme.colorScheme.error,
            container = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            message = state.message,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector?,
    tint: Color,
    container: Color,
    message: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(
                        color = tint,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = tint)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = Muted)
                    }
                }
                if (actionLabel != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel, color = BrandGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (loading) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PaymentSuccess(orderId: Int?, onContinueShopping: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = BrandGreen
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Payment successful",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = buildString {
                append("Thank you for shopping with Tamaade.")
                if (orderId != null) append(" Your order #").append(orderId).append(" is confirmed.")
                else append(" Your order is confirmed.")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinueShopping,
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
        ) {
            Text("Continue shopping")
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                model = line.product.image,
                contentDescription = line.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                Text(
                    text = formatPrice(line.product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = line.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalIconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove")
            }
        }
    }
}
