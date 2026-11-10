import SwiftUI

/// Account deletion (App Store requirement): POST api/user/delete-account/ with the user's password.
struct DeleteAccountView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router
    @Environment(\.dismiss) private var dismiss

    @State private var password = ""
    @State private var localError: String?

    private var contact: String? {
        store.user?.email.nonBlank ?? store.user?.phoneNumber.nonBlank
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 40))
                    .foregroundColor(.errorRed)
                Text("Permanently delete your Tamaade account?")
                    .font(.tHeadline)
                    .foregroundColor(.foreground)
                    .padding(.top, 12)
                Text("This erases your account" + (contact.map { " (\($0))" } ?? "") +
                     " and the personal data linked to it, including your profile, saved basket and delivery " +
                     "details. You will be signed out on this device. This cannot be undone.")
                    .font(.tBody)
                    .foregroundColor(.muted)
                    .padding(.top, 8)
                Text("Enter your password to confirm")
                    .font(.tTitle.weight(.regular))
                    .foregroundColor(.foreground)
                    .padding(.top, 24)
                BrandTextField(
                    placeholder: "Password",
                    systemImage: "lock",
                    text: Binding(get: { password }, set: {
                        password = $0
                        localError = nil
                        store.clearDeleteAccountError()
                    }),
                    isSecure: true,
                    contentType: .password
                )
                .padding(.top, 8)
                if let error = localError ?? store.deleteAccountError {
                    Text(error)
                        .font(.tBodySmall)
                        .foregroundColor(.errorRed)
                        .padding(.top, 8)
                }
                HStack {
                    Spacer()
                    Button("Forgot password?") {
                        router.presentForgotPassword(phone: store.user?.phoneNumber ?? "")
                    }
                    .foregroundColor(.brandGreen)
                    .disabled(store.deletingAccount)
                }
                .padding(.vertical, 8)
                PrimaryButton(title: store.deletingAccount ? "Deleting…" : "Delete my account",
                              loading: store.deletingAccount, enabled: !store.deletingAccount,
                              color: .errorRed) {
                    if password.isBlank {
                        localError = "Enter your password"
                    } else {
                        store.deleteAccount(password: password) { message in
                            router.resetToHome()
                            router.showToast(message)
                        }
                    }
                }
                .padding(.top, 8)
                Button("Keep my account") { dismiss() }
                    .foregroundColor(.muted)
                    .disabled(store.deletingAccount)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .padding(24)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.pageBackground)
        .navigationTitle("Delete account")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(store.deletingAccount)
        .brandNavigationBar()
        .onAppear { store.clearDeleteAccountError() }
    }
}
