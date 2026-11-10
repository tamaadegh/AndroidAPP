import SwiftUI

/// Bottom tabs (mirrors TamaadeWeb mobileNavItems).
enum AppTab: Hashable, CaseIterable {
    case home, categories, deals, cart, profile

    var label: String {
        switch self {
        case .home: return "Home"
        case .categories: return "Category"
        case .deals: return "Deals"
        case .cart: return "Cart"
        case .profile: return "Me"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house"
        case .categories: return "square.grid.2x2"
        case .deals: return "tag"
        case .cart: return "cart"
        case .profile: return "person"
        }
    }
}

/// Screens pushed on a tab's navigation stack.
enum Route: Hashable {
    case product(id: Int)
    case productList(ProductListQuery)
    case privacy
    case editProfile
    case deleteAccount
}

/// Screens of the sign-in sheet.
enum AuthRoute: Hashable {
    case login(phone: String)
    case signUp(phone: String)
    case forgotPassword(phone: String)
    case privacy
}

/// The sign-in sheet: where it starts and what is pushed on it.
struct AuthFlow: Identifiable {
    let id = UUID()
    var root: AuthRoute
}

/// Tab selection, one navigation stack per tab, the sign-in sheet and toasts.
@MainActor
final class Router: ObservableObject {
    @Published var tab: AppTab = .home
    @Published var paths: [AppTab: [Route]] = [:]
    @Published var authFlow: AuthFlow?
    @Published var authPath: [AuthRoute] = []
    @Published private(set) var toast: Toast?

    struct Toast: Identifiable, Equatable {
        let id = UUID()
        let text: String
    }

    private var toastTask: Task<Void, Never>?

    func path(for tab: AppTab) -> Binding<[Route]> {
        Binding(get: { self.paths[tab] ?? [] }, set: { self.paths[tab] = $0 })
    }

    func open(_ tab: AppTab) {
        self.tab = tab
    }

    func push(_ route: Route) {
        paths[tab, default: []].append(route)
    }

    func pop() {
        if !(paths[tab] ?? []).isEmpty { paths[tab]?.removeLast() }
    }

    /// Back to Home with every stack emptied (after deleting the account).
    func resetToHome() {
        paths = [:]
        tab = .home
    }

    // MARK: Sign-in sheet

    func presentLogin(phone: String = "") {
        guard authFlow == nil else { return }
        authPath = []
        authFlow = AuthFlow(root: .login(phone: phone))
    }

    func presentForgotPassword(phone: String) {
        authPath = []
        authFlow = AuthFlow(root: .forgotPassword(phone: phone))
    }

    func dismissAuth() {
        authFlow = nil
        authPath = []
    }

    /// "Sign up" from Login: never stacks a second Sign up.
    func showSignUp(phone: String) {
        if let index = authPath.firstIndex(where: { if case .signUp = $0 { return true } else { return false } }) {
            authPath.removeSubrange(index...)
        }
        authPath.append(.signUp(phone: phone))
    }

    /// "Sign in" from Sign up: back to a fresh Login with the phone filled in.
    func showLogin(phone: String) {
        authPath = []
        authFlow?.root = .login(phone: phone)
    }

    // MARK: Toasts

    func showToast(_ text: String) {
        toastTask?.cancel()
        withAnimation { toast = Toast(text: text) }
        toastTask = Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            guard !Task.isCancelled else { return }
            withAnimation { toast = nil }
        }
    }
}
