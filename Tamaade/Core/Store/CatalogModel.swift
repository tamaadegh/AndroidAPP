import Foundation

/// Everything the storefront shows, straight from the public catalog endpoints.
struct CatalogData {
    var products: [Product] = []
    var categories: [ProductCategory] = []
    var banners: [HeroBanner] = []
    var priceTiers: [PriceTier] = []
    var promos: [StorefrontPromo] = []
    var mainTiles: [MerchTile] = []
    var featuredTiles: [MerchTile] = []
    var bottomTiles: [MerchTile] = []
    var homeSections: [HomeSection] = []
    var dealsSections: [HomeSection] = []

    func promo(_ key: String) -> StorefrontPromo? { promos.first { $0.key == key } }

    func product(id: Int) -> Product? { products.first { $0.id == id } }

    /// Discounted / promo products, used on Deals when the backend has no deals sections.
    var deals: [Product] { products.filter(\.isDeal) }
}

/// Sort keys carried by product-list links (mirrors TamaadeWeb /products?ordering=… / ?filter=…).
enum ProductSort {
    static let `default` = ""
    static let newest = "newest"
    static let top = "top"
}

@MainActor
final class CatalogModel: ObservableObject {
    @Published private(set) var data = CatalogData()
    /// First load (or retry after an error) with nothing on screen yet.
    @Published private(set) var loading = true
    /// Pull-to-refresh in progress.
    @Published private(set) var refreshing = false
    @Published private(set) var error: String?
    @Published private(set) var loadedOnce = false
    /// Bumped after every successful products load so the basket can be re-synced.
    @Published private(set) var productsVersion = 0

    private let api: TamaadeAPI
    private var loadTask: Task<Void, Never>?

    init(api: TamaadeAPI = .shared, autoload: Bool = true) {
        self.api = api
        if autoload { load(pull: false) } else { loading = false }
    }

    /// Pull-to-refresh; returns when the load is done (for `.refreshable`).
    func refresh() async {
        load(pull: true)
        await loadTask?.value
    }

    func retry() { load(pull: false) }

    private func load(pull: Bool) {
        if loadTask != nil { return }
        if pull { refreshing = true } else { loading = true }
        error = nil
        let api = self.api
        loadTask = Task { [weak self] in
            do {
                async let products = api.products()
                async let categories = Self.optional { try await api.categories() }
                async let banners = Self.optional { try await api.banners() }
                async let tiers = Self.optional { try await api.priceTiers() }
                async let promos = Self.optional { try await api.promos() }
                async let main = Self.optional { try await api.merchTiles(placement: "main") }
                async let featured = Self.optional { try await api.merchTiles(placement: "featured") }
                async let bottom = Self.optional { try await api.merchTiles(placement: "bottom") }
                async let home = Self.optional { try await api.homeSections(location: "home") }
                async let deals = Self.optional { try await api.homeSections(location: "deals") }
                let loaded = CatalogData(
                    products: try await products,
                    categories: await categories,
                    banners: await banners,
                    priceTiers: await tiers,
                    promos: await promos,
                    mainTiles: await main,
                    featuredTiles: await featured,
                    bottomTiles: await bottom,
                    homeSections: await home,
                    dealsSections: await deals
                )
                self?.data = loaded
                self?.loadedOnce = true
                self?.productsVersion += 1
            } catch {
                self?.error = APIError.from(error).message
            }
            self?.loading = false
            self?.refreshing = false
            self?.loadTask = nil
        }
    }

    /// Product from the loaded list, else GET api/products/<id>/.
    func findProduct(id: Int) async -> Product? {
        if let product = data.product(id: id) { return product }
        return try? await api.product(id: id)
    }

    /// Secondary storefront content is optional: a failing endpoint just hides its section.
    nonisolated private static func optional<T>(_ block: () async throws -> [T]) async -> [T] {
        (try? await block()) ?? []
    }
}

/// Same rules as TamaadeWeb productsForSection().
func productsForSection(_ source: String, _ products: [Product]) -> [Product] {
    switch source {
    case "newest": return Array(sortNewest(products).prefix(12))
    // No sales data in the API yet: show the catalog order rather than a fake ranking.
    default: return Array(products.prefix(12))
    }
}

func sortNewest(_ products: [Product]) -> [Product] {
    products.enumerated()
        .sorted { a, b in
            let (ca, cb) = (a.element.createdAt ?? "", b.element.createdAt ?? "")
            return ca != cb ? ca > cb : a.offset < b.offset
        }
        .map { $0.element }
}

/// Client-side filtering, matching TamaadeWeb /products.
func filterProducts(_ products: [Product], category: String = "", maxPrice: Int = -1,
                    query: String = "", sort: String = ProductSort.default) -> [Product] {
    var result = products
    if !query.isBlank {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        result = result.filter {
            $0.name.lowercased().contains(q) ||
                $0.desc.lowercased().contains(q) ||
                $0.category.lowercased().contains(q) ||
                $0.brand.lowercased().contains(q)
        }
    }
    if !category.isBlank {
        let cat = category.lowercased()
        result = result.filter {
            let pc = $0.category.lowercased()
            return !pc.isEmpty && (pc.contains(cat) || cat.contains(pc))
        }
    }
    if maxPrice > 0 { result = result.filter { $0.priceValue <= Double(maxPrice) } }
    return sort == ProductSort.newest ? sortNewest(result) : result
}
