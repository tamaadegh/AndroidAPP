import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var catalog: CatalogModel
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router
    @StateObject private var feed = EndlessFeed(keyPrefix: "home_feed")

    private var data: CatalogData { catalog.data }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                content
            }
            .padding(.bottom, 24)
        }
        .background(Color.pageBackground)
        .refreshable { await catalog.refresh() }
        .onAppear { feed.update(source: data.products, loop: true) }
        .onChange(of: catalog.productsVersion) { _ in feed.update(source: data.products, loop: true) }
    }

    @ViewBuilder
    private var content: some View {
        let nothingLoaded = data.products.isEmpty && data.banners.isEmpty
        if catalog.loading && nothingLoaded {
            LoadingState()
        } else if let error = catalog.error, nothingLoaded {
            ErrorState(message: error, onRetry: catalog.retry)
        } else {
            if let promo = data.promo("top_strip") {
                PromoBanner(title: promo.title, highlight: promo.highlight, ctaLabel: promo.ctaLabel) {
                    openLink(promo.link, "/deals")
                }
            }
            if !data.banners.isEmpty {
                HeroCarousel(banners: data.banners) { openLink($0.link, "/deals") }
                    .padding(.top, 12)
            }
            if !data.priceTiers.isEmpty {
                SectionHeader(title: "Shop by budget", subtitle: "Great finds under every price")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(data.priceTiers) { tier in
                            Button {
                                router.push(.productList(ProductListQuery(maxPrice: Int(tier.amount))))
                            } label: {
                                VStack(spacing: 2) {
                                    Text("Under").font(.tLabelSmall).foregroundColor(.foreground)
                                    Text(PriceFormat.format(tier.amount))
                                        .font(.tTitle.weight(.bold))
                                        .foregroundColor(.brandGreen)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.7)
                                }
                                .padding(14)
                                .frame(width: 100)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }
            MerchRow(tiles: data.mainTiles, onOpen: openLink)
            MerchRow(tiles: data.featuredTiles, onOpen: openLink)
            ForEach(data.homeSections) { section in
                let products = productsForSection(section.productSource, data.products)
                if !products.isEmpty {
                    SectionHeader(
                        title: section.title,
                        subtitle: section.subtitle,
                        actionLabel: section.ctaLabel.isBlank ? "View all" : section.ctaLabel,
                        onAction: { openLink(section.link, "/products") }
                    )
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(alignment: .top, spacing: 12) {
                            ForEach(products) { product in
                                ProductCard(
                                    product: product,
                                    onTap: { router.push(.product(id: product.id)) },
                                    onAddToCart: { add(product) }
                                )
                                .frame(width: 168)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
            }
            MerchRow(tiles: data.bottomTiles, onOpen: openLink)
            exploreFeed
            let hasContent = !data.products.isEmpty || !data.banners.isEmpty ||
                !data.mainTiles.isEmpty || !data.featuredTiles.isEmpty
            if !hasContent {
                EmptyState(
                    title: "The store is getting ready",
                    message: "There are no products yet. Pull down to refresh or check back soon.",
                    actionLabel: "Refresh",
                    onAction: { Task { await catalog.refresh() } }
                )
            }
        }
    }

    /// Endless "Explore" feed: every product, two per row, loading more as you scroll.
    @ViewBuilder
    private var exploreFeed: some View {
        if !feed.entries.isEmpty {
            SectionHeader(title: "Explore", subtitle: "Keep scrolling for more finds")
            let rows = stride(from: 0, to: feed.entries.count, by: 2).map {
                Array(feed.entries[$0..<min($0 + 2, feed.entries.count)])
            }
            ForEach(rows, id: \.first!.id) { row in
                HStack(alignment: .top, spacing: 12) {
                    ForEach(row) { entry in
                        ProductCard(
                            product: entry.product,
                            onTap: { router.push(.product(id: entry.product.id)) },
                            onAddToCart: { add(entry.product) }
                        )
                        .frame(maxWidth: .infinity)
                        .onAppear { feed.onAppear(entry) }
                    }
                    if row.count == 1 { Color.clear.frame(maxWidth: .infinity) }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
            }
            if feed.hasMore {
                FeedLoadingFooter { feed.loadMore() }
            }
        }
    }

    private func openLink(_ link: String?, _ fallback: String) {
        router.open(link: link, default: fallback, categories: data.categories)
    }

    private func add(_ product: Product) {
        store.addToCart(product)
        router.showToast("\(product.name) added to basket")
    }
}

private struct MerchRow: View {
    let tiles: [MerchTile]
    let onOpen: (String?, String) -> Void

    var body: some View {
        if !tiles.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 4) {
                    ForEach(tiles) { tile in
                        CategoryChip(title: tile.title, imageURL: tile.image, badge: tile.badge,
                                     highlight: tile.highlight) {
                            onOpen(tile.link, "/products")
                        }
                    }
                }
                .padding(.horizontal, 12)
            }
            .padding(.top, 12)
        }
    }
}

/// Auto-advancing banner pager (every 5 s), like the Android HorizontalPager.
private struct HeroCarousel: View {
    let banners: [HeroBanner]
    let onTap: (HeroBanner) -> Void
    @State private var page = 0

    private let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 8) {
            TabView(selection: $page) {
                ForEach(Array(banners.enumerated()), id: \.element.id) { index, banner in
                    Button { onTap(banner) } label: {
                        RemoteImage(url: banner.image, contentMode: .fill, size: .full)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .padding(.horizontal, 16)
                            .accessibilityLabel(banner.title.isBlank ? SiteConfig.name : banner.title)
                    }
                    .buttonStyle(.plain)
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: 160)
            if banners.count > 1 {
                PageDots(count: banners.count, current: page)
            }
        }
        .onReceive(timer) { _ in
            guard banners.count > 1 else { return }
            withAnimation { page = (page + 1) % banners.count }
        }
    }
}
