package com.tamaade.ecommerce.ui.util

import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.data.model.SiteConfig
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val priceFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

fun formatPrice(price: String): String {
    val value = price.toDoubleOrNull() ?: return "${SiteConfig.currency}$price"
    return "${SiteConfig.currency}${priceFormat.format(value)}"
}

fun formatPrice(value: Double): String = "${SiteConfig.currency}${priceFormat.format(value)}"

fun Product.displayImage(): String? = image
