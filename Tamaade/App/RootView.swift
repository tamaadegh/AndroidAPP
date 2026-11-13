import SwiftUI

struct RootView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var catalog: CatalogModel
    @EnvironmentObject private var router: Router
    @Environment(\.scenePhase) private var scenePhase

    @State private var showSplash = true
    @State private var browser = CheckoutBrowser()

    var body: some View {
        ZStack {
            MainTabView()
            if showSplash {
                SplashView()
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .overlay(alignment: .bottom) {
            if let toast = router.toast {
                ToastView(text: toast.text)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 64)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .id(toast.id)
            }
        }
        .sheet(item: $router.authFlow) { _ in
            AuthSheet()
                .environmentObject(store)
                .environmentObject(catalog)
                .environmentObject(router)
        }
        .task {
            // Show the splash at least 1.4 s, and use that time (up to 3.5 s) to load the
            // catalog and the first screen's images, so Home opens fully drawn.
            let started = Date()
            let warmedUp = SplashFlag()
            // Not awaited: downloads keep going in the background once the splash is gone.
            Task { @MainActor in
                await warmUpHome()
                warmedUp.done = true
            }
            while !(warmedUp.done && Date().timeIntervalSince(started) >= 1.4) &&
                Date().timeIntervalSince(started) < 3.5 {
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
            withAnimation(.easeOut(duration: 0.3)) { showSplash = false }
        }
        .onAppear { store.music.onForeground() }
        .onReceive(store.events) { handle($0) }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                store.music.onForeground()
                // If the Hubtel page never redirected back to the app, check the pending payment anyway.
                store.onAppResumed()
            case .background:
                store.music.onBackground()
            default:
                break
            }
        }
        // Keep the saved basket in line with the live catalog (removed products, new prices).
        .onChange(of: catalog.productsVersion) { _ in
            if catalog.loadedOnce {
                store.syncCartWithCatalog(catalog.data.products)
                ImagePrefetcher.prefetchCatalog(catalog.data)
            }
        }
    }

    /// Waits for the catalog (saved copy or network), then decodes Home's first images.
    private func warmUpHome() async {
        let started = Date()
        while catalog.loading && Date().timeIntervalSince(started) < 3.5 {
            try? await Task.sleep(nanoseconds: 50_000_000)
        }
        await ImagePrefetcher.warmUpHome(catalog.data)
    }

    private func handle(_ event: StoreEvent) {
        switch event {
        case .message(let text):
            router.showToast(text)
        case .openCheckout(let url):
            store.checkoutPageOpen = true
            let opened = browser.open(url) { callback in
                store.checkoutPageOpen = false
                if let callback {
                    store.handleDeepLink(callback)
                } else {
                    // Sheet closed without the redirect: they may still have paid.
                    store.onAppResumed()
                }
            }
            if !opened {
                store.checkoutPageOpen = false
                // Fall back to Safari; the return comes back through onOpenURL.
                UIApplication.shared.open(url) { ok in
                    if !ok { router.showToast("Couldn't open Hubtel checkout") }
                }
            }
        case .requireLogin:
            router.presentLogin()
        case .showCart:
            router.open(.cart)
        }
    }
}

/// Set once Home's first images are ready (read by the splash loop on the main actor).
@MainActor
private final class SplashFlag {
    var done = false
}

struct MainTabView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    var body: some View {
        TabView(selection: $router.tab) {
            ForEach(AppTab.allCases, id: \.self) { tab in
                NavigationStack(path: router.path(for: tab)) {
                    root(for: tab)
                        .navigationDestination(for: Route.self) { RouteView(route: $0) }
                }
                .tabItem { Label(tab.label, systemImage: tab.systemImage) }
                .tag(tab)
                .badge(tab == .cart ? min(store.cartCount, 99) : 0)
            }
        }
        .tint(.brandGreen)
    }

    @ViewBuilder
    private func root(for tab: AppTab) -> some View {
        switch tab {
        case .home: HomeView().storeTopBar()
        case .categories: CategoriesView().storeTopBar()
        case .deals: DealsView().storeTopBar()
        case .cart: CartView()
        case .profile: ProfileView().storeTopBar()
        }
    }
}

struct RouteView: View {
    let route: Route

    var body: some View {
        switch route {
        case .product(let id): ProductDetailView(productId: id)
        case .productList(let query): ProductListView(filters: query)
        case .privacy: PrivacyView()
        case .editProfile: EditProfileView()
        case .deleteAccount: DeleteAccountView()
        }
    }
}

/// Green store header: logo + tagline, and a search button that opens all products.
struct StoreTopBar: ViewModifier {
    @EnvironmentObject private var router: Router

    func body(content: Content) -> some View {
        content
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 0) {
                        TamaadeWordmark(height: 26)
                        Text(SiteConfig.tagline)
                            .font(.system(size: 10, weight: .medium))
                            .foregroundColor(.white.opacity(0.85))
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        router.push(.productList(ProductListQuery()))
                    } label: {
                        Image(systemName: "magnifyingglass")
                    }
                    .accessibilityLabel("Search")
                }
            }
            .brandNavigationBar()
    }
}

extension View {
    func storeTopBar() -> some View { modifier(StoreTopBar()) }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Color.brandGreen.ignoresSafeArea()
            VStack(spacing: 16) {
                TamaadeWordmark(height: 120)
                Text(SiteConfig.tagline)
                    .font(.system(size: 13))
                    .foregroundColor(.cream.opacity(0.85))
            }
            .padding(.horizontal, 32)
        }
    }
}

struct ToastView: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundColor(.white)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(hex: 0x323232))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .shadow(color: .black.opacity(0.2), radius: 6, y: 2)
            .accessibilityAddTraits(.isStaticText)
    }
}

extension Router {
    /// Admin-configured links (banners, tiles, promos, "view all") -> app screens.
    func open(link: String?, default fallback: String, categories: [ProductCategory]) {
        let raw = link?.nonBlank ?? fallback
        switch StoreLink.resolve(raw, categories: categories) {
        case .home: open(.home)
        case .deals: open(.deals)
        case .categories: open(.categories)
        case .cart: open(.cart)
        case .profile: open(.profile)
        case .privacy: push(.privacy)
        case .product(let id): push(.product(id: id))
        case .productList(let query): push(.productList(query))
        case .external(let url): UIApplication.shared.open(url)
        }
    }
}
