package com.tamaade.ecommerce.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.ui.navigation.TopLevelDestination
import com.tamaade.ecommerce.ui.theme.BrandGreen

@Composable
fun TamaadeBottomBar(
    currentRoute: String?,
    cartCount: Int,
    onNavigate: (TopLevelDestination) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        TopLevelDestination.entries.forEach { dest ->
            val selected = currentRoute == dest.route
            val (filled, outlined) = iconsFor(dest)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = {
                    if (dest is TopLevelDestination.Cart && cartCount > 0) {
                        BadgedBox(
                            badge = { Badge { Text(cartCount.coerceAtMost(99).toString()) } }
                        ) {
                            Icon(
                                imageVector = if (selected) filled else outlined,
                                contentDescription = dest.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) filled else outlined,
                            contentDescription = dest.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(dest.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandGreen,
                    selectedTextColor = BrandGreen,
                    indicatorColor = BrandGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}

private fun iconsFor(dest: TopLevelDestination): Pair<ImageVector, ImageVector> = when (dest) {
    TopLevelDestination.Home -> Icons.Filled.Home to Icons.Outlined.Home
    TopLevelDestination.Categories -> Icons.Filled.GridView to Icons.Outlined.GridView
    TopLevelDestination.Deals -> Icons.Filled.LocalOffer to Icons.Outlined.LocalOffer
    TopLevelDestination.Cart -> Icons.Filled.ShoppingCart to Icons.Outlined.ShoppingCart
    TopLevelDestination.Profile -> Icons.Filled.Person to Icons.Outlined.Person
}
