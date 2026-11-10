import SwiftUI

/// Error "code" from api/user/otp/request/ for a number with no account.
private let notRegistered = "not_registered"

/// Sign in. Phone number + SMS code is the default; email + password stays available
/// for existing email accounts ("Sign in with email instead").
struct LoginView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    @State private var useEmail = false
    @State private var phone: String
    /// Number the code was sent to (server's +233 form); nil while on the phone step.
    @State private var codeSentTo: String?
    @State private var code = ""
    @State private var info: String?
    @State private var email = ""
    @State private var password = ""
    @State private var localError: String?
    @State private var now = ProcessInfo.processInfo.systemUptime

    init(initialPhone: String) {
        _phone = State(initialValue: initialPhone)
    }

    private var shownError: String? { localError ?? store.authError }
    private var normalizedPhone: String? { PhoneNumber.normalizeGhana(phone) }
    private var cooldown: Int {
        _ = now
        return store.otpCooldownSeconds(for: codeSentTo ?? normalizedPhone)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                AuthHeader(title: "Welcome back", subtitle: subtitle)
                if useEmail {
                    emailStep
                } else if let sentTo = codeSentTo {
                    OtpCodeStep(
                        phone: sentTo,
                        code: Binding(get: { code }, set: { code = $0; info = nil; edited() }),
                        loading: store.authLoading,
                        error: shownError,
                        info: info,
                        resendSeconds: cooldown,
                        verifyLabel: "Sign In",
                        onVerify: { typed in
                            store.verifyOtp(phone: sentTo, code: typed) { router.signedIn("Signed in") }
                        },
                        onResend: {
                            edited()
                            store.requestOtp(phone: sentTo) { _ in
                                code = ""
                                info = "A new code is on its way."
                            }
                        },
                        onChangeNumber: {
                            codeSentTo = nil
                            code = ""
                            edited()
                        }
                    )
                } else {
                    phoneStep
                }

                if codeSentTo == nil || useEmail {
                    Button(useEmail ? "Sign in with phone number instead" : "Sign in with email instead") {
                        useEmail.toggle()
                        edited()
                    }
                    .foregroundColor(.brandGreen)
                    .disabled(store.authLoading)
                    .padding(.top, 16)
                }
                HStack(spacing: 4) {
                    Text("Don't have an account?").foregroundColor(.muted)
                    Button("Sign up") {
                        edited()
                        router.showSignUp(phone: useEmail ? "" : phone.trimmingCharacters(in: .whitespaces))
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.brandGreen)
                }
                .font(.system(size: 15))
                .padding(.top, 16)
                Button("Continue as guest") { router.dismissAuth() }
                    .foregroundColor(.muted)
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

    private var subtitle: String {
        if useEmail { return "Sign in to checkout and track your Tamaade orders." }
        if codeSentTo != nil { return "Enter the code from the SMS to sign in." }
        return "Sign in with your phone number. We'll text you a code."
    }

    private var phoneStep: some View {
        VStack(spacing: 0) {
            PhoneNumberField(text: Binding(get: { phone }, set: { phone = $0; edited() }))
                .padding(.bottom, 16)
            AuthErrorBlock(
                error: shownError,
                actionLabel: localError == nil && store.authErrorCode == notRegistered ? "Create an account" : nil,
                onAction: {
                    edited()
                    router.showSignUp(phone: normalizedPhone ?? phone.trimmingCharacters(in: .whitespaces))
                }
            )
            PrimaryButton(
                title: store.authLoading ? "Sending code…" : cooldown > 0 ? "Send code (\(cooldown)s)" : "Send code",
                loading: store.authLoading,
                enabled: !store.authLoading && cooldown == 0,
                action: sendCode
            )
        }
    }

    private var emailStep: some View {
        VStack(spacing: 0) {
            BrandTextField(placeholder: "Email", systemImage: "envelope",
                           text: Binding(get: { email }, set: { email = $0; edited() }),
                           keyboard: .emailAddress, contentType: .username, autocapitalization: .never)
            BrandTextField(placeholder: "Password", systemImage: "lock",
                           text: Binding(get: { password }, set: { password = $0; edited() }),
                           isSecure: true, showToggle: true, contentType: .password)
                .padding(.top, 14)
            HStack {
                Spacer()
                Button("Forgot password?") {
                    edited()
                    router.authPath.append(.forgotPassword(phone: ""))
                }
                .foregroundColor(.brandGreen)
                .disabled(store.authLoading)
            }
            .padding(.vertical, 10)
            AuthErrorBlock(error: shownError)
            PrimaryButton(title: store.authLoading ? "Signing in…" : "Sign In",
                          loading: store.authLoading, enabled: !store.authLoading) {
                if email.isBlank || password.isBlank {
                    localError = "Enter email and password"
                } else {
                    store.login(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password) {
                        router.signedIn("Signed in")
                    }
                }
            }
        }
    }

    private func sendCode() {
        guard let target = normalizedPhone else {
            localError = "Enter a valid Ghana mobile number, e.g. 024 123 4567"
            return
        }
        store.requestOtp(phone: target) { result in
            codeSentTo = result.phoneNumber
            code = ""
            info = nil
        }
    }

    private func edited() {
        localError = nil
        store.clearAuthError()
    }
}

/// Step 2 of phone sign-in: enter the 6-digit SMS code. Verifies automatically once
/// all digits are typed; `onVerify` is also wired to the button for retries.
struct OtpCodeStep: View {
    let phone: String
    @Binding var code: String
    let loading: Bool
    let error: String?
    let info: String?
    let resendSeconds: Int
    let verifyLabel: String
    let onVerify: (String) -> Void
    let onResend: () -> Void
    let onChangeNumber: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("We sent a 6-digit code to")
                .font(.tBody)
                .foregroundColor(.muted)
            HStack {
                Text(phone)
                    .font(.tTitle.weight(.bold))
                    .foregroundColor(.foreground)
                Spacer()
                Button("Change number", action: onChangeNumber)
                    .foregroundColor(.brandGreen)
                    .disabled(loading)
            }
            .padding(.bottom, 8)
            BrandTextField(
                placeholder: "6-digit code",
                systemImage: "message",
                text: Binding(get: { code }, set: { input in
                    let digits = PhoneNumber.filterCode(input)
                    let completed = digits.count == PhoneNumber.otpLength && digits != code
                    code = digits
                    if completed && !loading { onVerify(digits) }
                }),
                keyboard: .numberPad,
                contentType: .oneTimeCode
            )
            .padding(.bottom, 16)
            AuthErrorBlock(error: error)
            if error == nil, let info {
                Text(info)
                    .font(.tBodySmall)
                    .foregroundColor(.brandGreen)
                    .padding(.bottom, 8)
            }
            PrimaryButton(title: loading ? "Verifying…" : verifyLabel, loading: loading,
                          enabled: !loading && code.count == PhoneNumber.otpLength) {
                onVerify(code)
            }
            ResendRow(seconds: resendSeconds, loading: loading, onResend: onResend)
        }
    }
}
