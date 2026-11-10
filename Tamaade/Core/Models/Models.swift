import Foundation

/// A product from GET api/products/. Every field comes from the backend.
struct Product: Identifiable, Hashable {
    let id: Int
    let name: String
    let desc: String
    let category: String
    let seller: String
    /// Display image: primary/first of images[], else image; nil shows the logo fallback.
    let image: String?
    /// All gallery image URLs in display order (may be empty).
    let images: [String]
    let price: String
    let compareAtPrice: String?
    let discountPercent: Int?
    let promoLabel: String
    let brand: String
    let isNew: Bool
    let isExpress: Bool
    let saleEndsAt: String?
    let quantity: Int
    let createdAt: String?

    var priceValue: Double { Double(price) ?? 0 }

    /// compare_at_price only when it is really higher than the price (same rule as TamaadeWeb).
    var originalPrice: String? {
        guard let compare = compareAtPrice, (Double(compare) ?? 0) > priceValue else { return nil }
        return compare
    }

    /// discount_percent from the API, else derived from compare_at_price.
    var effectiveDiscount: Int? {
        if let discountPercent { return discountPercent }
        guard let original = originalPrice.flatMap(Double.init), original > 0 else { return nil }
        let pct = Int(((1 - priceValue / original) * 100).rounded(.toNearestOrAwayFromZero))
        return pct > 0 ? pct : nil
    }

    var isDeal: Bool { effectiveDiscount != nil || !promoLabel.isBlank }

    var inStock: Bool { quantity > 0 }
}

struct ProductCategory: Identifiable, Hashable {
    let id: Int
    let name: String
    let icon: String?
}

/// GET api/products/merch-tiles/?placement=main|featured|bottom
struct MerchTile: Identifiable, Hashable {
    let id: Int
    let placement: String
    let title: String
    let image: String?
    let link: String?
    let badge: String
    let highlight: Bool
}

struct PriceTier: Identifiable, Hashable {
    let id: Int
    let amount: Double
}

/// GET api/products/banners/ - image-only hero slides.
struct HeroBanner: Identifiable, Hashable {
    let id: Int
    let title: String
    let image: String
    let link: String?
}

/// GET api/products/promos/ - e.g. key "top_strip" (home) and "deals_header" (deals).
struct StorefrontPromo: Identifiable, Hashable {
    let id: Int
    let key: String
    let title: String
    let subtitle: String
    let highlight: String
    let ctaLabel: String
    let link: String?
}

/// GET api/products/home-sections/?location=home|deals
struct HomeSection: Identifiable, Hashable {
    let id: Int
    let key: String
    let location: String
    let title: String
    let subtitle: String
    let ctaLabel: String
    let link: String?
    /// "newest", "featured" or "bestsellers" (see `productsForSection`).
    let productSource: String
}

struct CartLine: Identifiable, Hashable {
    let product: Product
    let quantity: Int

    var id: Int { product.id }
    var lineTotal: Double { product.priceValue * Double(quantity) }
}

/// `email` can be empty for accounts created with a phone number (and `phoneNumber` for email accounts).
struct UserSession: Equatable {
    let email: String
    let firstName: String
    let lastName: String
    var id: Int? = nil
    var phoneNumber: String = ""

    var displayName: String {
        let full = "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)
        if !full.isEmpty { return full }
        return email.isBlank ? phoneNumber : email
    }
}

/// Response of POST api/user/payments/hubtel/checkout/
struct HubtelCheckout {
    let checkoutUrl: String
    let checkoutDirectUrl: String?
    let checkoutId: String?
    let clientReference: String
    let orderId: Int?
    let amount: String?
}

/// Response of GET api/user/payments/hubtel/status/ - status is "P" (pending), "C" (complete) or "F" (failed).
struct PaymentStatus {
    let status: String
    let paid: Bool
    let orderId: Int?
    let clientReference: String
}

struct PrivacyContent {
    let title: String
    let content: String
    let updatedAt: String?
}

struct MusicTrack: Equatable {
    let id: Int?
    let title: String
    let url: String
    let volume: Float
}

struct BackgroundMusicConfig {
    let enabled: Bool
    let track: MusicTrack?
}

struct AuthResult {
    let accessToken: String
    /// Long-lived token used to get a new access token when it expires (nil if the server sent none).
    let refreshToken: String?
    let user: UserSession
}

/// Response of POST api/user/token/refresh/ (`refreshToken` is set only when the server rotates it).
struct RefreshedTokens {
    let accessToken: String
    let refreshToken: String?
}

/// Response of POST api/user/otp/request/ (`phoneNumber` is the server's +233 form).
struct OtpRequestResult {
    let phoneNumber: String
    let expiresIn: Int
    let resendIn: Int
    let message: String
}

/// Where the Hubtel checkout is, from tapping "Checkout" to the payment being confirmed.
enum CheckoutState: Equatable {
    /// Nothing in flight.
    case idle
    /// Creating the Hubtel checkout on the backend.
    case starting
    /// Hubtel page open; waiting for the user to come back.
    case awaitingPayment(reference: String)
    /// Asking the backend whether the payment went through.
    case verifying(reference: String)
    /// Backend has not confirmed the payment yet; the user can check again.
    case pending(reference: String, message: String)
    case paid(reference: String, orderId: Int?)
    /// Payment failed or was cancelled; the basket is kept so the user can retry.
    case failed(message: String)

    var isBusy: Bool {
        switch self {
        case .starting, .verifying: return true
        default: return false
        }
    }
}

enum SiteConfig {
    static let name = "Tamaade"
    static let tagline = "Online Shopping in Ghana"
    static let currency = "GH₵"
    static let country = "Ghana"
}

extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    /// nil for a blank string (Kotlin's `ifBlank { null }`).
    var nonBlank: String? { isBlank ? nil : self }
}
