import SwiftUI

struct ProductDetailView: View {
    let productId: Int

    @EnvironmentObject private var catalog: CatalogModel
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router
    @Environment(\.dismiss) private var dismiss

    @State private var fetched: Product?
    @State private var fetching = true

    private var product: Product? { catalog.data.product(id: productId) ?? fetched }

    var body: some View {
        Group {
            if let product {
                detail(product)
            } else if catalog.loading || fetching {
                LoadingState()
                    .frame(maxHeight: .infinity)
            } else {
                EmptyState(title: "Product not found", message: "This product may have been removed.",
                           actionLabel: "Go back", onAction: { dismiss() })
                    .frame(maxHeight: .infinity)
            }
        }
        .background(Color.pageBackground)
        .navigationTitle(product?.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .brandNavigationBar()
        .task(id: catalog.productsVersion) {
            if product == nil { fetched = await catalog.findProduct(id: productId) }
            fetching = false
        }
    }

    private func detail(_ product: Product) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ProductGallery(product: product)
                if !product.promoLabel.isBlank {
                    Text(product.promoLabel)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.brandGreen)
                        .frame(maxWidth: .infinity)
                        .padding(10)
                        .background(LinearGradient.promoStrip)
                }
                VStack(alignment: .leading, spacing: 0) {
                    if !product.brand.isBlank {
                        Text(product.brand.uppercased())
                            .font(.tLabel.weight(.bold))
                            .foregroundColor(.brandGreen)
                    }
                    Text(product.name)
                        .font(.tHeadline)
                        .foregroundColor(.foreground)
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(PriceFormat.format(product.price))
                            .font(.tHeadline)
                            .foregroundColor(.foreground)
                        if let pct = product.effectiveDiscount {
                            Text("-\(pct)%")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.brandGreen)
                        }
                    }
                    .padding(.top, 8)
                    if product.isExpress {
                        Badge(text: "express delivery", capsule: true)
                            .padding(.top, 8)
                    }
                    Text("Overview")
                        .font(.tTitle.weight(.bold))
                        .foregroundColor(.foreground)
                        .padding(.top, 16)
                    Text(product.desc)
                        .font(.tBodyLarge)
                        .foregroundColor(.foreground)
                        .padding(.top, 6)
                    if !product.category.isBlank {
                        Text("Category · \(product.category)")
                            .font(.tBody)
                            .foregroundColor(.muted)
                            .padding(.top, 8)
                    }
                    if !product.seller.isBlank {
                        Text("Sold by \(product.seller)")
                            .font(.tBody)
                            .foregroundColor(.muted)
                    }
                }
                .padding(16)
                .padding(.bottom, 8)
            }
        }
        .safeAreaInset(edge: .bottom) { buyBar(product) }
    }

    private func buyBar(_ product: Product) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 0) {
                Text(PriceFormat.format(product.price))
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.foreground)
                if let original = product.originalPrice {
                    Text(PriceFormat.format(original))
                        .font(.tBodySmall)
                        .foregroundColor(.strikeGray)
                        .strikethrough()
                }
            }
            Spacer()
            Button {
                store.addToCart(product)
                router.showToast("Added to basket")
            } label: {
                Text(product.inStock ? "Add to basket" : "Sold out")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .frame(height: 48)
                    .background(product.inStock ? Color.brandGreen : Color.brandGreen.opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            .disabled(!product.inStock)
        }
        .padding(16)
        .background(Color.white.shadow(.drop(color: .black.opacity(0.08), radius: 6, y: -2)))
    }
}

/// Product images from the API (images[] then image); the logo shows when there are none.
private struct ProductGallery: View {
    let product: Product
    @State private var page = 0

    var body: some View {
        let urls: [String?] = product.images.isEmpty ? [product.image] : product.images.map { Optional($0) }
        ZStack(alignment: .bottom) {
            Color.cardImageBackground
            TabView(selection: $page) {
                ForEach(urls.indices, id: \.self) { i in
                    RemoteImage(url: urls[i])
                        .padding(24)
                        .tag(i)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            if urls.count > 1 {
                PageDots(count: urls.count, current: page)
                    .padding(.bottom, 10)
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}
