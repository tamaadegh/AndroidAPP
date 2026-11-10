import SwiftUI

/// Forgotten password, by SMS: phone number -> 6-digit code + new password. On success the
/// server signs the user in (POST api/user/password/reset/otp/confirm/).
struct ForgotPasswordView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router
    @Environment(\.dismiss) private var dismiss

    @State private var phone: String
    @State private var codeSentTo: String?
    @State private var code = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var info: String?
    @State private var localError: String?
    @State private var now = ProcessInfo.processInfo.systemUptime

    init(initialPhone: String) {
        _phone = State(initialValue: initialPhone)
    }

    private var fieldErrors: [String: String] { store.authFieldErrors }
    private var shownError: String? { localError ?? store.authError }
    private var normalizedPhone: String? { PhoneNumber.normalizeGhana(phone) }
    private var cooldown: Int {
        _ = now
        return store.otpCooldownSeconds(for: codeSentTo ?? normalizedPhone)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                AuthHeader(
                    title: "Reset your password",
                    subtitle: codeSentTo == nil
                        ? "Enter the phone number on your account. We'll text you a code."
                        : "Enter the code from the SMS and choose a new password."
                )
                if let sentTo = codeSentTo {
                    resetStep(sentTo)
                } else {
                    phoneStep
                }
                Button("Back") {
                    if codeSentTo != nil {
                        codeSentTo = nil
                        code = ""
                        edited()
                    } else if router.authPath.isEmpty {
                        router.dismissAuth()
                    } else {
                        dismiss()
                    }
                }
                .foregroundColor(.muted)
                .disabled(store.authLoading)
                .padding(.vertical, 12)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.pageBackground)
        .navigationBarTitleDisplayMode(.inline)
        .cooldownClock($now)
        .onAppear { store.clearAuthError() }
    }

    private var phoneStep: some View {
        VStack(spacing: 0) {
            PhoneNumberField(text: Binding(get: { phone }, set: { phone = $0; edited() }))
            FieldError(message: fieldErrors["phone_number"])
            AuthErrorBlock(error: shownError)
                .padding(.top, 16)
            PrimaryButton(
                title: store.authLoading ? "Sending code…" : cooldown > 0 ? "Send code (\(cooldown)s)" : "Send code",
                loading: store.authLoading,
                enabled: !store.authLoading && cooldown == 0
            ) {
                guard let target = normalizedPhone else {
                    localError = "Enter a valid Ghana mobile number, e.g. 024 123 4567"
                    return
                }
                store.requestPasswordReset(phone: target) { result in
                    codeSentTo = result.phoneNumber
                    code = ""
                }
            }
            Text("Your account has no phone number? Sign in with your e-mail and password, " +
                 "then add a phone number in Profile → Edit details.")
                .font(.tBodySmall)
                .foregroundColor(.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 12)
        }
    }

    private func resetStep(_ sentTo: String) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(sentTo)
                    .font(.tTitle.weight(.bold))
                    .foregroundColor(.foreground)
                Spacer()
                Button("Change number") {
                    codeSentTo = nil
                    code = ""
                    edited()
                }
                .foregroundColor(.brandGreen)
                .disabled(store.authLoading)
            }
            .padding(.bottom, 8)
            BrandTextField(placeholder: "6-digit code", systemImage: "message",
                           text: Binding(get: { code }, set: { code = PhoneNumber.filterCode($0); edited() }),
                           keyboard: .numberPad, contentType: .oneTimeCode)
            FieldError(message: fieldErrors["code"])
            BrandTextField(placeholder: "New password", systemImage: "lock",
                           text: Binding(get: { password }, set: { password = $0; edited() }),
                           isSecure: true, showToggle: true, contentType: .newPassword)
                .padding(.top, 14)
            FieldError(message: fieldErrors["new_password"])
            BrandTextField(placeholder: "Confirm new password", systemImage: "lock",
                           text: Binding(get: { confirmPassword }, set: { confirmPassword = $0; edited() }),
                           isSecure: true, contentType: .newPassword)
                .padding(.top, 14)
            Text("At least \(Validation.minPasswordLength) characters.")
                .font(.tBodySmall)
                .foregroundColor(.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)
                .padding(.bottom, 16)
            AuthErrorBlock(error: shownError)
            if shownError == nil, let info {
                Text(info)
                    .font(.tBodySmall)
                    .foregroundColor(.brandGreen)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.bottom, 8)
            }
            PrimaryButton(title: store.authLoading ? "Saving…" : "Reset password",
                          loading: store.authLoading, enabled: !store.authLoading) {
                if code.count != PhoneNumber.otpLength {
                    localError = "Enter the 6-digit code we sent you."
                } else if password.count < Validation.minPasswordLength {
                    localError = "Password must be at least \(Validation.minPasswordLength) characters"
                } else if password != confirmPassword {
                    localError = "Passwords don't match"
                } else {
                    localError = nil
                    store.confirmPasswordReset(phone: sentTo, code: code, newPassword: password) {
                        router.signedIn("Password updated — you're signed in")
                    }
                }
            }
            ResendRow(seconds: cooldown, loading: store.authLoading) {
                edited()
                store.requestPasswordReset(phone: sentTo) { _ in
                    code = ""
                    info = "A new code is on its way."
                }
            }
        }
    }

    private func edited() {
        localError = nil
        info = nil
        store.clearAuthError()
    }
}
