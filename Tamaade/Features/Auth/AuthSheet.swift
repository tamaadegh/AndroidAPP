import SwiftUI

/// The sign-in sheet: Login, Sign up and Forgot password on their own navigation stack.
struct AuthSheet: View {
    @EnvironmentObject private var router: Router

    var body: some View {
        NavigationStack(path: $router.authPath) {
            if let root = router.authFlow?.root {
                // Sign in / Sign up / Reset password draw their own green header with a back arrow.
                screen(root)
                    .id(root)
                    .navigationDestination(for: AuthRoute.self) { screen($0) }
            }
        }
        .tint(.brandGreen)
    }

    @ViewBuilder
    private func screen(_ route: AuthRoute) -> some View {
        switch route {
        case .login(let phone): LoginView(initialPhone: phone)
        case .signUp(let phone): SignUpView(initialPhone: phone)
        case .forgotPassword(let phone): ForgotPasswordView(initialPhone: phone)
        case .privacy: PrivacyView()
        }
    }
}

extension Router {
    /// Leaves the sign-in sheet back to where the user came from.
    func signedIn(_ message: String) {
        dismissAuth()
        showToast(message)
    }
}
