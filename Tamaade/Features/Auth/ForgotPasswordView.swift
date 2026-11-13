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
        _phone = State(initialValue: PhoneNumber.ghanaLocalDigits(initialPhone))
    }

    private var fieldErrors: [String: String] { store.authFieldErrors }
    private var shownError: String? { localError ?? store.authError }
    private var normalizedPhone: String? { PhoneNumber.normalizeGhana(phone) }
    private var cooldown: Int {
        _ = now
        return store.otpCooldownSeconds(for: codeSentTo ?? normalizedPhone)
    }

    var body: some View {
        AuthScaffold(
            title: "Reset your password",
            subtitle: codeSentTo == nil
                ? "Enter the phone number on your account. We'll text you a code."
                : "Enter the code from the SMS and choose a new password.",
            onBack: {
                if codeSentTo != nil { backToPhoneStep() } else if router.authPath.isEmpty { router.dismissAuth() } else { dismiss() }
            },
            backEnabled: !store.authLoading
        ) {
            if let sentTo = codeSentTo {
                resetStep(sentTo)
            } else {
                phoneStep
            }
        }
        .animation(.easeOut(duration: 0.2), value: shownError)
        .cooldownClock($now)
        .onAppear { store.clearAuthError() }
    }

    private var phoneStep: some View {
        VStack(alignment: .leading, spacing: 0) {
            FieldLabel("Phone number")
            GhanaPhoneField(
                digits: Binding(get: { phone }, set: { phone = $0; edited() }),
                isError: localError != nil || fieldErrors["phone_number"] != nil
            )
            FieldError(message: fieldErrors["phone_number"])
                .padding(.bottom, 14)
            AuthNotice(message: shownError)
            PrimaryButton(
                title: store.authLoading ? "Sending code…" : cooldown > 0 ? "Send code (\(cooldown)s)" : "Send code",
                loading: store.authLoading,
                enabled: !store.authLoading && cooldown == 0
            ) {
                guard let target = normalizedPhone else {
                    localError = "That number doesn't look right. Enter a Ghana mobile number, e.g. 24 123 4567."
                    return
                }
                store.requestPasswordReset(phone: target) { result in
                    codeSentTo = result.phoneNumber
                    code = ""
                }
            }
            .padding(.bottom, 14)
            AuthNotice(
                message: "No phone number on your account? Sign in with your email and password, " +
                    "then add one in Profile → Edit details.",
                kind: .info
            )
        }
    }

    private func resetStep(_ sentTo: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            CodeSentRow(phone: sentTo, loading: store.authLoading, onChange: backToPhoneStep)
            OtpBoxes(code: Binding(get: { code }, set: { code = $0; edited() }),
                     isError: fieldErrors["code"] != nil, enabled: !store.authLoading)
            FieldError(message: fieldErrors["code"])
            FieldLabel("New password").padding(.top, 18)
            BrandTextField(placeholder: "New password", systemImage: "lock",
                           text: Binding(get: { password }, set: { password = $0; edited() }),
                           isSecure: true, showToggle: true, contentType: .newPassword)
            FieldError(message: fieldErrors["new_password"])
            FieldLabel("Confirm new password").padding(.top, 14)
            BrandTextField(placeholder: "Confirm new password", systemImage: "lock",
                           text: Binding(get: { confirmPassword }, set: { confirmPassword = $0; edited() }),
                           isSecure: true, contentType: .newPassword)
            Text("At least \(Validation.minPasswordLength) characters.")
                .font(.tBodySmall)
                .foregroundColor(.muted)
                .padding(.top, 4)
                .padding(.bottom, 16)
            AuthNotice(message: shownError)
            AuthNotice(message: shownError == nil ? info : nil, kind: .success)
            PrimaryButton(title: store.authLoading ? "Saving…" : "Reset password",
                          loading: store.authLoading, enabled: !store.authLoading) {
                if code.count != PhoneNumber.otpLength {
                    localError = "Enter the 6-digit code we sent you."
                } else if password.count < Validation.minPasswordLength {
                    localError = "Your new password needs at least \(Validation.minPasswordLength) characters."
                } else if password != confirmPassword {
                    localError = "The two passwords don't match."
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
            .padding(.top, 8)
        }
    }

    private func backToPhoneStep() {
        codeSentTo = nil
        code = ""
        edited()
    }

    private func edited() {
        localError = nil
        info = nil
        store.clearAuthError()
    }
}
