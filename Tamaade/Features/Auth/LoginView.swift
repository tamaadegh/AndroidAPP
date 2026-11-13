import SwiftUI

/// Error "code" from api/user/otp/request/ for a number with no account.
private let notRegistered = "not_registered"

/// Sign in. Phone number + SMS code is the default; email + password stays available
/// for existing email accounts ("Continue with email").
struct LoginView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    @State private var useEmail = false
    /// The 9 local digits after +233.
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
        _phone = State(initialValue: PhoneNumber.ghanaLocalDigits(initialPhone))
    }

    private var loading: Bool { store.authLoading }
    private var shownError: String? { localError ?? store.authError }
    private var normalizedPhone: String? { PhoneNumber.normalizeGhana(phone) }
    private var cooldown: Int {
        _ = now
        return store.otpCooldownSeconds(for: codeSentTo ?? normalizedPhone)
    }

    var body: some View {
        AuthScaffold(
            title: useEmail ? "Sign in with email" : codeSentTo != nil ? "Check your messages" : "Welcome back",
            subtitle: useEmail ? "Use the email and password of your Tamaade account."
                : codeSentTo != nil ? "Enter the 6-digit code we just sent you." : nil,
            onBack: {
                if !useEmail && codeSentTo != nil { backToPhoneStep() } else { router.authBack() }
            },
            content: {
                if useEmail {
                    emailStep
                } else if let sentTo = codeSentTo {
                    OtpCodeStep(
                        phone: sentTo,
                        code: Binding(get: { code }, set: { code = $0; info = nil; edited() }),
                        loading: loading,
                        error: shownError,
                        info: info,
                        resendSeconds: cooldown,
                        verifyLabel: "Sign in",
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
                        onChangeNumber: backToPhoneStep
                    )
                } else {
                    phoneStep
                }
            },
            footer: {
                HStack(spacing: 4) {
                    Text("New to Tamaade?").foregroundColor(.muted)
                    Button("Create an account") {
                        edited()
                        router.showSignUp(phone: useEmail ? "" : normalizedPhone ?? phone)
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.brandGreen)
                }
                .font(.system(size: 15))
                .padding(.vertical, 8)
                Button("Continue as guest") { router.dismissAuth() }
                    .foregroundColor(.muted)
                    .padding(.vertical, 8)
            }
        )
        .animation(.easeOut(duration: 0.2), value: shownError)
        .cooldownClock($now)
        .onAppear { store.clearAuthError() }
    }

    private var phoneStep: some View {
        VStack(alignment: .leading, spacing: 0) {
            FieldLabel("Phone number")
            GhanaPhoneField(
                digits: Binding(get: { phone }, set: { phone = $0; edited() }),
                isError: localError != nil,
                onSubmit: { if !loading && cooldown == 0 { sendCode() } }
            )
            .padding(.bottom, 14)
            let serverError = localError == nil ? store.authError : nil
            if let serverError, NoticeKind.classify(serverError) == .unavailable {
                AuthNotice(
                    message: "Signing in with a phone number isn't switched on yet. Use your email and password for now.",
                    kind: .unavailable,
                    actionLabel: "Use email instead",
                    onAction: { useEmail = true; edited() }
                )
            } else if let serverError, store.authErrorCode == notRegistered {
                AuthNotice(
                    message: serverError,
                    title: "No account with this number",
                    actionLabel: "Create an account",
                    onAction: {
                        edited()
                        router.showSignUp(phone: normalizedPhone ?? phone)
                    }
                )
            } else {
                AuthNotice(message: shownError)
            }
            PrimaryButton(
                title: loading ? "Sending code…" : cooldown > 0 ? "Send code again in \(cooldown)s" : "Send code",
                loading: loading,
                enabled: !loading && cooldown == 0,
                action: sendCode
            )
            OrDivider()
            SwitchMethodButton(title: "Continue with email", systemImage: "envelope", enabled: !loading) {
                useEmail = true
                edited()
            }
        }
    }

    private var emailStep: some View {
        VStack(alignment: .leading, spacing: 0) {
            FieldLabel("Email")
            BrandTextField(placeholder: "you@example.com", systemImage: "envelope",
                           text: Binding(get: { email }, set: { email = $0; edited() }),
                           keyboard: .emailAddress, contentType: .username, autocapitalization: .never)
                .padding(.bottom, 14)
            FieldLabel("Password")
            BrandTextField(placeholder: "Your password", systemImage: "lock",
                           text: Binding(get: { password }, set: { password = $0; edited() }),
                           isSecure: true, showToggle: true, contentType: .password)
            HStack {
                Spacer()
                Button("Forgot password?") {
                    edited()
                    router.authPath.append(.forgotPassword(phone: ""))
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(.brandGreen)
                .disabled(loading)
            }
            .padding(.vertical, 10)
            AuthNotice(
                message: shownError,
                title: localError == nil && shownError.map({ NoticeKind.classify($0) == .error }) == true
                    ? "Couldn't sign you in" : nil
            )
            PrimaryButton(title: loading ? "Signing in…" : "Sign in", loading: loading, enabled: !loading) {
                let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
                if trimmed.isEmpty && password.isBlank {
                    localError = "Enter your email and password."
                } else if trimmed.isEmpty {
                    localError = "Enter your email."
                } else if password.isBlank {
                    localError = "Enter your password."
                } else {
                    store.login(email: trimmed, password: password) { router.signedIn("Signed in") }
                }
            }
            OrDivider()
            SwitchMethodButton(title: "Continue with phone number", systemImage: "iphone", enabled: !loading) {
                useEmail = false
                edited()
            }
        }
    }

    private func sendCode() {
        guard let target = normalizedPhone else {
            localError = "That number doesn't look right. Enter a Ghana mobile number, e.g. 24 123 4567."
            return
        }
        store.requestOtp(phone: target) { result in
            codeSentTo = result.phoneNumber
            code = ""
            info = nil
        }
    }

    private func backToPhoneStep() {
        codeSentTo = nil
        code = ""
        edited()
    }

    private func edited() {
        localError = nil
        store.clearAuthError()
    }
}
