import SwiftUI

struct ProductListView: View {
    let filters: ProductListQuery

    @EnvironmentObject private var catalog: CatalogModel
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router
    @State private var query: String

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    init(filters: ProductListQuery) {
        self.filters = filters
        _query = State(initialValue: filters.query)
    }

    private var title: String {
        if !filters.category.isBlank { return filters.category }
        if filters.maxPrice > 0 { return "Under \(PriceFormat.format(Double(filters.maxPrice)))" }
        return "All products"
    }

    var body: some View {
        let products = filterProducts(catalog.data.products, category: filters.category,
                                      maxPrice: filters.maxPrice, query: query, sort: filters.sort)
        ScrollView {
            VStack(spacing: 0) {
                BrandTextField(placeholder: "Search products", systemImage: "magnifyingglass", text: $query,
                               autocapitalization: .never)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                if products.isEmpty {
                    if catalog.loading {
                        LoadingState()
                    } else if let error = catalog.error {
                        ErrorState(message: error, onRetry: catalog.retry)
                    } else {
                        EmptyState(title: "No products found", message: "Try another search or category.")
                    }
                } else {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(products) { product in
                            ProductCard(
                                product: product,
                                onTap: { router.push(.product(id: product.id)) },
                                onAddToCart: {
                                    store.addToCart(product)
                                    router.showToast("\(product.name) added to basket")
                                }
                            )
                        }
                    }
                    .padding(16)
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.pageBackground)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text(title)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                    Text("\(products.count) items")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
        }
        .brandNavigationBar()
    }
}
