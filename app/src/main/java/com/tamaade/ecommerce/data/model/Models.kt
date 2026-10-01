package com.tamaade.ecommerce.data.model

/** A product from GET api/products/. Every field comes from the backend. */
data class Product(
    val id: Int,
    val name: String,
    val desc: String,
    val category: String,
    val seller: String,
    /** Display image: primary/first of images[], else image; null shows the logo fallback. */
    val image: String?,
    /** All gallery image URLs in display order (may be empty). */
    val images: List<String>,
    val price: String,
    val compareAtPrice: String?,
    val discountPercent: Int?,
    val promoLabel: String,
    val brand: String,
    val isNew: Boolean,
    val isExpress: Boolean,
    val saleEndsAt: String?,
    val quantity: Int,
    val createdAt: String?
) {
    val priceValue: Double
        get() = price.toDoubleOrNull() ?: 0.0

    /** compare_at_price only when it is really higher than the price (same rule as TamaadeWeb). */
    val originalPrice: String?
        get() = compareAtPrice?.takeIf { (it.toDoubleOrNull() ?: 0.0) > priceValue }

    /** discount_percent from the API, else derived from compare_at_price. */
    val effectiveDiscount: Int?
        get() = discountPercent ?: originalPrice?.toDoubleOrNull()?.let { original ->
            Math.round((1 - priceValue / original) * 100).toInt().takeIf { it > 0 }
        }

    val isDeal: Boolean
        get() = effectiveDiscount != null || promoLabel.isNotBlank()
}

data class ProductCategory(
    val id: Int,
    val name: String,
    val icon: String?
)

/** GET api/products/merch-tiles/?placement=main|featured|bottom */
data class MerchTile(
    val id: Int,
    val placement: String,
    val title: String,
    val image: String?,
    val link: String?,
    val badge: String,
    val highlight: Boolean
)

data class PriceTier(
    val id: Int,
    val amount: Double
)

/** GET api/products/banners/ — image-only hero slides. */
data class HeroBanner(
    val id: Int,
    val title: String,
    val image: String,
    val link: String?
)

/** GET api/products/promos/ — e.g. key "top_strip" (home) and "deals_header" (deals). */
data class StorefrontPromo(
    val id: Int,
    val key: String,
    val title: String,
    val subtitle: String,
    val highlight: String,
    val ctaLabel: String,
    val link: String?
)

/** GET api/products/home-sections/?location=home|deals */
data class HomeSection(
    val id: Int,
    val key: String,
    val location: String,
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val link: String?,
    /** "newest", "featured" or "bestsellers" (see productsForSection). */
    val productSource: String
)

data class CartLine(
    val product: Product,
    val quantity: Int
) {
    val lineTotal: Double
        get() = (product.price.toDoubleOrNull() ?: 0.0) * quantity
}

data class UserSession(
    val email: String,
    val firstName: String,
    val lastName: String,
    val id: Int? = null
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifBlank { email }
}

/** Response of POST api/user/payments/hubtel/checkout/ */
data class HubtelCheckout(
    val checkoutUrl: String,
    val checkoutDirectUrl: String?,
    val checkoutId: String?,
    val clientReference: String,
    val orderId: Int?,
    val amount: String?
)

/** Response of GET api/user/payments/hubtel/status/ — status is "P" (pending), "C" (complete) or "F" (failed). */
data class PaymentStatus(
    val status: String,
    val paid: Boolean,
    val orderId: Int?,
    val clientReference: String
)

data class PrivacyContent(
    val title: String,
    val content: String,
    val updatedAt: String?
)

data class MusicTrack(
    val id: Int?,
    val title: String,
    val url: String,
    val volume: Float
)

data class BackgroundMusicConfig(
    val enabled: Boolean,
    val track: MusicTrack?
)

/** Where the Hubtel checkout is, from tapping "Checkout" to the payment being confirmed. */
sealed interface CheckoutState {
    /** Nothing in flight. */
    data object Idle : CheckoutState

    /** Creating the Hubtel checkout on the backend. */
    data object Starting : CheckoutState

    /** Hubtel page opened in the browser; waiting for the user to come back. */
    data class AwaitingPayment(val reference: String) : CheckoutState

    /** Asking the backend whether the payment went through. */
    data class Verifying(val reference: String) : CheckoutState

    /** Backend has not confirmed the payment yet; the user can check again. */
    data class Pending(val reference: String, val message: String) : CheckoutState

    data class Paid(val reference: String, val orderId: Int?) : CheckoutState

    /** Payment failed or was cancelled; the basket is kept so the user can retry. */
    data class Failed(val message: String) : CheckoutState
}

object SiteConfig {
    const val name = "Tamaade"
    const val tagline = "Online Shopping in Ghana"
    const val currency = "GH₵"
    const val country = "Ghana"
}
