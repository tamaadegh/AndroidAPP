import SwiftUI

struct CategoriesView: View {
    @EnvironmentObject private var catalog: CatalogModel
    @EnvironmentObject private var router: Router

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        ScrollView {
            let categories = catalog.data.categories
            if categories.isEmpty {
                if catalog.loading {
                    LoadingState()
                } else if let error = catalog.error {
                    ErrorState(message: error, onRetry: catalog.retry)
                } else {
                    EmptyState(title: "No categories yet",
                               message: "Categories will appear here once products are added.")
                }
            } else {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(categories) { category in
                        Button {
                            router.push(.productList(ProductListQuery(category: category.name)))
                        } label: {
                            CategoryTile(category: category)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
        }
        .background(Color.pageBackground)
        .refreshable { await catalog.refresh() }
    }
}

private struct CategoryTile: View {
    let category: ProductCategory

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle().fill(Color.cardImageBackground)
                if category.icon != nil {
                    RemoteImage(url: category.icon, contentMode: .fill)
                        .clipShape(Circle())
                } else {
                    Text(category.name.prefix(1).uppercased())
                        .font(.tHeadline)
                        .foregroundColor(.brandGreen)
                }
            }
            .frame(width: 64, height: 64)
            Text(category.name)
                .font(.tTitle)
                .foregroundColor(.foreground)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .aspectRatio(1.1, contentMode: .fit)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
