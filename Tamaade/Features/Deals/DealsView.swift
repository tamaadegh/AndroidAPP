import SwiftUI

/// Mirrors TamaadeWeb /deals: "deals_header" promo + home-sections with location=deals.
struct DealsView: View {
    @EnvironmentObject private var catalog: CatalogModel
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        let data = catalog.data
        // Sections configured for the deals page; without any, list the products that are on offer.
        let sections = data.dealsSections
            .map { DealSection(section: $0, products: productsForSection($0.productSource, data.products)) }
            .filter { !$0.products.isEmpty }
        let fallbackDeals = sections.isEmpty ? data.deals : []

        ScrollView {
            if catalog.loading && data.products.isEmpty {
                LoadingState()
            } else if let error = catalog.error, data.products.isEmpty {
                ErrorState(message: error, onRetry: catalog.retry)
            } else {
                VStack(spacing: 12) {
                    header(data.promo("deals_header"))
                    ForEach(sections) { entry in
                        let section = entry.section
                        SectionHeader(
                            title: section.title,
                            subtitle: section.subtitle,
                            actionLabel: section.ctaLabel.isBlank ? "View all" : section.ctaLabel,
                            onAction: { openLink(section.link, "/products") }
                        )
                        .padding(.horizontal, -16)
                        grid(entry.products)
                    }
                    if !fallbackDeals.isEmpty {
                        grid(fallbackDeals)
                    }
                    if sections.isEmpty && fallbackDeals.isEmpty {
                        EmptyState(title: "No deals right now",
                                   message: "New offers will appear here. Pull down to refresh.")
                    }
                }
                .padding(16)
            }
        }
        .background(Color.pageBackground)
        .refreshable { await catalog.refresh() }
    }

    @ViewBuilder
    private func header(_ promo: StorefrontPromo?) -> some View {
        if let promo {
            Button { openLink(promo.link, "/products") } label: {
                VStack(spacing: 4) {
                    Text(promo.title)
                        .font(.system(size: 22, weight: .black))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    if !promo.subtitle.isBlank {
                        Text(promo.subtitle)
                            .font(.tBody)
                            .foregroundColor(.white.opacity(0.9))
                            .multilineTextAlignment(.center)
                    }
                    Text(promo.ctaLabel.isBlank ? "SHOP NOW" : promo.ctaLabel)
                        .font(.tLabel.weight(.bold))
                        .foregroundColor(.brandGreen)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(Color.white)
                        .clipShape(Capsule())
                        .padding(.top, 8)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(Color.brandGreen)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
        } else {
            VStack(alignment: .leading, spacing: 2) {
                Text("Deals")
                    .font(.tHeadline)
                    .foregroundColor(.brandGreen)
                Text("Current offers across the store")
                    .font(.tBody)
                    .foregroundColor(.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 4)
        }
    }

    private func grid(_ products: [Product]) -> some View {
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
    }

    private func openLink(_ link: String?, _ fallback: String) {
        router.open(link: link, default: fallback, categories: catalog.data.categories)
    }
}

private struct DealSection: Identifiable {
    let section: HomeSection
    let products: [Product]

    var id: Int { section.id }
}
