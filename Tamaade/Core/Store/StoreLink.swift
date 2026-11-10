import Foundation

/// Filters for the product list screen (category, budget, search, sort).
struct ProductListQuery: Hashable {
    var category: String = ""
    var maxPrice: Int = -1
    var query: String = ""
    var sort: String = ProductSort.default
}

/// Where an admin-configured link (banner, tile, promo, section "view all") should take the user.
enum StoreLink: Equatable {
    case home
    case deals
    case categories
    case cart
    case profile
    case privacy
    case productList(ProductListQuery)
    case product(id: Int)
    case external(URL)

    /// Maps TamaadeWeb-style paths ("/deals", "/products?category=Shoes", "/products/12",
    /// "/category/3", "/allcategories", ...) to app screens. Absolute URLs open in the browser.
    static func resolve(_ link: String?, categories: [ProductCategory],
                        fallback: StoreLink = .productList(ProductListQuery())) -> StoreLink {
        let raw = (link ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if raw.isEmpty { return fallback }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            if let url = URL(string: raw) { return .external(url) }
            return fallback
        }

        let path = raw.hasPrefix("/") ? raw : "/" + raw
        guard let components = URLComponents(string: path)
            ?? path.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed).flatMap(URLComponents.init(string:))
        else { return fallback }
        let segments = components.path.split(separator: "/").map(String.init)
        func param(_ name: String) -> String {
            // Like Android's Uri.getQueryParameter, "+" reads as a space.
            (components.queryItems?.first { $0.name == name }?.value ?? "")
                .replacingOccurrences(of: "+", with: " ")
        }

        switch segments.first ?? "" {
        case "":
            return .home
        case "deals":
            return .deals
        case "allcategories", "categories":
            return .categories
        case "cart", "checkout":
            return .cart
        case "profile", "login", "register":
            return .profile
        case "privacy":
            return .privacy
        case "category":
            let id = segments.count > 1 ? Int(segments[1]) : nil
            if let name = categories.first(where: { $0.id == id })?.name {
                return .productList(ProductListQuery(category: name))
            }
            return .categories
        case "products":
            if segments.count > 1, let id = Int(segments[1]) {
                return .product(id: id)
            }
            let ordering = param("ordering")
            let filter = param("filter")
            let sort: String
            if ordering == "-created_at" || filter == "back-to-stock" {
                sort = ProductSort.newest
            } else if filter == "top-selling" || filter == "top-picks" {
                sort = ProductSort.top
            } else {
                sort = ProductSort.default
            }
            let search = param("search")
            return .productList(ProductListQuery(
                category: param("category"),
                maxPrice: Double(param("maxPrice")).map { Int($0) } ?? -1,
                query: search.isBlank ? param("q") : search,
                sort: sort
            ))
        default:
            return fallback
        }
    }
}
