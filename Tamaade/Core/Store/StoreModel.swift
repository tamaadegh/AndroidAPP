import Combine
import Foundation

/// One-off things the UI should do in response to the store (toasts, navigation, opening Hubtel).
enum StoreEvent {
    case message(String)
    case openCheckout(URL)
    case requireLogin
    case showCart
}

/// The basket, the signed-in session and the Hubtel checkout - the iOS port of Android's
/// `StoreViewModel`, with the same rules.
@MainActor
final class StoreModel: ObservableObject {
    @Published private(set) var cartLines: [CartLine]
    @Published private(set) var user: UserSession?

    @Published private(set) var authLoading = false
    @Published private(set) var authError: String?
    /// The "code" of the last auth error (e.g. "not_registered").
    @Published private(set) var authErrorCode: String?
    /// Per-field messages of the last auth error (e.g. "password" for a too-weak sign-up password).
    @Published private(set) var authFieldErrors: [String: String] = [:]
    /// Phone number (+233 form) whose SMS code can't be re-sent before `otpCooldownUntil`.
    @Published private(set) var otpCooldownPhone: String?
    /// `ProcessInfo.systemUptime` at which another code may be requested for `otpCooldownPhone`.
    @Published private(set) var otpCooldownUntil: TimeInterval = 0

    @Published private(set) var checkoutState: CheckoutState
    @Published private(set) var musicEnabled: Bool

    @Published private(set) var deletingAccount = false
    @Published private(set) var deleteAccountError: String?
    @Published private(set) var savingProfile = false
    @Published private(set) var profileError: String?
    /// Per-field messages from PATCH api/user/ (first_name, last_name, email, phone_number).
    @Published private(set) var profileFieldErrors: [String: String] = [:]

    /// True while the Hubtel page is on screen (the app-resume fallback then waits).
    var checkoutPageOpen = false

    let events = PassthroughSubject<StoreEvent, Never>()
    let music = BackgroundMusicPlayer()

    private let session: SessionStore
    private let api: TamaadeAPI
    private var verifyTask: Task<Void, Never>?
    /// One token refresh at a time, so parallel 401s don't each spend the refresh token.
    private var refreshTask: Task<String?, Error>?

    var cartCount: Int { cartLines.reduce(0) { $0 + $1.quantity } }
    var cartTotal: Double { cartLines.reduce(0) { $0 + $1.lineTotal } }

    init(session: SessionStore = SessionStore(), api: TamaadeAPI = .shared, loadMusic: Bool = true) {
        self.session = session
        self.api = api
        cartLines = session.loadCart()
        user = session.loadUser()
        musicEnabled = session.musicEnabled
        if let ref = session.pendingCheckoutRef {
            checkoutState = .awaitingPayment(reference: ref)
        } else {
            checkoutState = .idle
        }
        music.setUserEnabled(musicEnabled)
        if loadMusic {
            Task {
                // No music when the backend is unreachable or the endpoint isn't deployed yet.
                guard let config = try? await api.backgroundMusic() else { return }
                music.setTrack(config.enabled ? config.track : nil)
            }
        }
    }

    // MARK: - Cart

    func addToCart(_ product: Product, quantity: Int = 1) {
        if cartLines.contains(where: { $0.product.id == product.id }) {
            updateCart(cartLines.map {
                $0.product.id == product.id ? CartLine(product: $0.product, quantity: $0.quantity + quantity) : $0
            })
        } else {
            updateCart(cartLines + [CartLine(product: product, quantity: quantity)])
        }
    }

    func setQuantity(productId: Int, quantity: Int) {
        if quantity <= 0 {
            updateCart(cartLines.filter { $0.product.id != productId })
        } else {
            updateCart(cartLines.map {
                $0.product.id == productId ? CartLine(product: $0.product, quantity: quantity) : $0
            })
        }
    }

    func increment(productId: Int) {
        guard let line = cartLines.first(where: { $0.product.id == productId }) else { return }
        setQuantity(productId: productId, quantity: line.quantity + 1)
    }

    func decrement(productId: Int) {
        guard let line = cartLines.first(where: { $0.product.id == productId }) else { return }
        setQuantity(productId: productId, quantity: line.quantity - 1)
    }

    func removeFromCart(productId: Int) {
        updateCart(cartLines.filter { $0.product.id != productId })
    }

    func clearCart() { updateCart([]) }

    /// Re-syncs the saved basket with the live catalog: drops products that no longer exist
    /// or are out of stock, caps quantities to stock and refreshes names/prices/images.
    func syncCartWithCatalog(_ products: [Product]) {
        guard !cartLines.isEmpty else { return }
        let byId = Dictionary(products.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let synced: [CartLine] = cartLines.compactMap { line in
            guard let fresh = byId[line.product.id], fresh.quantity > 0 else { return nil }
            return CartLine(product: fresh, quantity: min(line.quantity, fresh.quantity))
        }
        guard synced != cartLines else { return }
        let removed = synced.count < cartLines.count
        let priceChanged = synced.contains { line in
            cartLines.first { $0.product.id == line.product.id }?.product.price != line.product.price
        }
        cartLines = synced
        session.saveCart(synced)
        if removed || priceChanged {
            events.send(.message(removed
                ? "Some basket items are no longer available and were removed."
                : "Basket prices were updated."))
        }
    }

    private func updateCart(_ lines: [CartLine]) {
        cartLines = lines
        session.saveCart(lines)
        // Editing the basket after a failed/finished attempt dismisses the old status banner.
        switch checkoutState {
        case .failed, .paid: checkoutState = .idle
        default: break
        }
    }

    // MARK: - Auth

    func clearAuthError() {
        authError = nil
        authErrorCode = nil
        authFieldErrors = [:]
    }

    func login(email: String, password: String, onSuccess: @escaping () -> Void) {
        signIn(onSuccess: onSuccess) { [api] in try await api.login(email: email, password: password) }
    }

    /// POST api/user/otp/request/ - texts a 6-digit sign-in code to `phone` (+233 form).
    func requestOtp(phone: String, onSent: @escaping (OtpRequestResult) -> Void) {
        sendCode(phone: phone, onSent: onSent) { [api] in try await api.requestOtp(phoneNumber: phone) }
    }

    /// Forgotten password, step 1: texts a reset code to `phone` (+233 form).
    func requestPasswordReset(phone: String, onSent: @escaping (OtpRequestResult) -> Void) {
        sendCode(phone: phone, onSent: onSent) { [api] in try await api.requestPasswordReset(phoneNumber: phone) }
    }

    /// Forgotten password, step 2: sets `newPassword` with the SMS `code` and signs the user in.
    func confirmPasswordReset(phone: String, code: String, newPassword: String, onSuccess: @escaping () -> Void) {
        signIn(phone: phone, onSuccess: onSuccess) { [api] in
            try await api.confirmPasswordReset(phoneNumber: phone, code: code, newPassword: newPassword)
        }
    }

    /// POST api/user/otp/verify/ - signs in with the SMS code.
    func verifyOtp(phone: String, code: String, onSuccess: @escaping () -> Void) {
        signIn(phone: phone, onSuccess: onSuccess) { [api] in try await api.verifyOtp(phoneNumber: phone, code: code) }
    }

    /// POST api/user/register/ - creates the account (email and/or phone + password) and
    /// signs it in. Per-field server errors end up in `authFieldErrors`.
    func register(firstName: String, lastName: String, password: String, email: String, phone: String,
                  onSuccess: @escaping () -> Void) {
        signIn(onSuccess: onSuccess) { [api] in
            try await api.register(firstName: firstName, lastName: lastName, password: password,
                                   email: email, phoneNumber: phone)
        }
    }

    /// Seconds left before another code can be requested for `phone`.
    func otpCooldownSeconds(for phone: String?, now: TimeInterval = ProcessInfo.processInfo.systemUptime) -> Int {
        guard let phone, phone == otpCooldownPhone else { return 0 }
        return max(0, Int((otpCooldownUntil - now).rounded(.up)))
    }

    private func sendCode(phone: String, onSent: @escaping (OtpRequestResult) -> Void,
                          call: @escaping () async throws -> OtpRequestResult) {
        guard !authLoading else { return }
        authLoading = true
        clearAuthError()
        Task {
            do {
                let result = try await call()
                startOtpCooldown(phone: phone, seconds: result.resendIn)
                if result.phoneNumber != phone { startOtpCooldown(phone: result.phoneNumber, seconds: result.resendIn) }
                onSent(result)
            } catch {
                onAuthFailed(APIError.from(error), phone: phone)
            }
            authLoading = false
        }
    }

    private func signIn(phone: String? = nil, onSuccess: @escaping () -> Void,
                        call: @escaping () async throws -> AuthResult) {
        guard !authLoading else { return }
        authLoading = true
        clearAuthError()
        Task {
            do {
                let result = try await call()
                session.saveSession(accessToken: result.accessToken, refreshToken: result.refreshToken, user: result.user)
                user = result.user
                otpCooldownPhone = nil
                onSuccess()
            } catch {
                onAuthFailed(APIError.from(error), phone: phone)
            }
            authLoading = false
        }
    }

    private func onAuthFailed(_ error: APIError, phone: String?) {
        authError = error.message
        authErrorCode = error.code
        authFieldErrors = error.fieldErrors
        if let phone, error.status == 429, let retryAfter = error.retryAfter {
            startOtpCooldown(phone: phone, seconds: retryAfter)
        }
    }

    private func startOtpCooldown(phone: String, seconds: Int) {
        otpCooldownPhone = phone
        otpCooldownUntil = ProcessInfo.processInfo.systemUptime + TimeInterval(max(seconds, 0))
    }

    func logout() {
        verifyTask?.cancel()
        verifyTask = nil
        refreshTask = nil
        session.clearSession()
        user = nil
        checkoutState = .idle
    }

    func clearProfileError() {
        profileError = nil
        profileFieldErrors = [:]
    }

    /// PATCH api/user/ with the edited names and contact details (`email`/`phone` "" removes it);
    /// on success the stored user is replaced with the server's copy.
    func updateProfile(firstName: String, lastName: String, email: String, phone: String,
                       onSaved: @escaping () -> Void) {
        guard session.accessToken != nil else {
            events.send(.requireLogin)
            return
        }
        guard !savingProfile else { return }
        savingProfile = true
        clearProfileError()
        Task {
            do {
                let updated = try await authorized { [api] token in
                    try await api.updateProfile(token: token, firstName: firstName, lastName: lastName,
                                                email: email, phoneNumber: phone)
                }
                session.saveUser(updated)
                user = updated
                onSaved()
            } catch {
                let e = APIError.from(error)
                if e.isUnauthorized {
                    expireSession()
                } else {
                    profileError = e.message
                    profileFieldErrors = e.fieldErrors
                }
            }
            savingProfile = false
        }
    }

    func clearDeleteAccountError() { deleteAccountError = nil }

    /// POST api/user/delete-account/; on success wipes the session, basket and pending checkout.
    func deleteAccount(password: String, onDeleted: @escaping (String) -> Void) {
        guard session.accessToken != nil else {
            events.send(.requireLogin)
            return
        }
        guard !deletingAccount else { return }
        deletingAccount = true
        deleteAccountError = nil
        Task {
            do {
                let message = try await authorized { [api] token in
                    try await api.deleteAccount(token: token, password: password)
                }
                logout()
                cartLines = []
                session.saveCart([])
                onDeleted(message)
            } catch {
                let e = APIError.from(error)
                if e.isUnauthorized { expireSession() } else { deleteAccountError = e.message }
            }
            deletingAccount = false
        }
    }

    /// Runs an authenticated call. On a 401 the access token is refreshed once with the stored
    /// refresh token and the call retried; if that fails too the original 401 is rethrown
    /// (callers then sign the user out).
    private func authorized<T>(_ call: @escaping (String) async throws -> T) async throws -> T {
        guard let token = session.accessToken else {
            throw APIError(message: "Please sign in.", status: 401)
        }
        do {
            return try await call(token)
        } catch let error as APIError where error.isUnauthorized {
            guard let fresh = try await refreshAccessToken(expired: token) else { throw error }
            return try await call(fresh)
        }
    }

    /// New access token, or nil when there is no usable refresh token (the session is over).
    private func refreshAccessToken(expired: String) async throws -> String? {
        if let running = refreshTask { return try await running.value }
        guard let current = session.accessToken else { return nil }
        // Another call already refreshed.
        if current != expired { return current }
        guard let refresh = session.refreshToken else { return nil }
        let task = Task<String?, Error> { [api, session] in
            do {
                let tokens = try await api.refreshTokens(refreshToken: refresh)
                // Signed out (or into another account) while the refresh was in flight.
                guard session.refreshToken == refresh else { return nil }
                session.updateTokens(accessToken: tokens.accessToken, refreshToken: tokens.refreshToken)
                return tokens.accessToken
            } catch let error as APIError where error.status == 401 || error.status == 400 {
                // The refresh token expired or was revoked. Network errors bubble up
                // so a flaky connection doesn't sign the user out.
                return nil
            }
        }
        refreshTask = task
        defer { refreshTask = nil }
        return try await task.value
    }

    private func expireSession() {
        logout()
        events.send(.message(APIError.sessionExpiredMessage))
        events.send(.requireLogin)
    }

    // MARK: - Hubtel checkout

    func startCheckout() {
        guard user != nil, session.accessToken != nil else {
            events.send(.requireLogin)
            return
        }
        guard !cartLines.isEmpty, checkoutState != .starting else { return }
        verifyTask?.cancel()
        checkoutState = .starting
        let lines = cartLines
        Task {
            do {
                let checkout = try await authorized { [api] token in
                    try await api.startHubtelCheckout(token: token, lines: lines)
                }
                guard let url = URL(string: checkout.checkoutUrl) else {
                    checkoutState = .failed(message: "Hubtel checkout could not be started. Please try again.")
                    return
                }
                session.pendingCheckoutRef = checkout.clientReference
                checkoutState = .awaitingPayment(reference: checkout.clientReference)
                events.send(.openCheckout(url))
            } catch {
                let e = APIError.from(error)
                if e.isUnauthorized { expireSession() } else { checkoutState = .failed(message: e.message) }
            }
        }
    }

    /// Deep link tamaade://checkout/result?ref=...&result=success|cancel from the backend return page.
    func handleDeepLink(_ url: URL) {
        guard url.scheme?.lowercased() == "tamaade", url.host?.lowercased() == "checkout" else { return }
        let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
        onCheckoutReturn(reference: items.first { $0.name == "ref" }?.value,
                         result: items.first { $0.name == "result" }?.value)
    }

    func onCheckoutReturn(reference: String?, result: String?) {
        guard let ref = reference?.nonBlank ?? session.pendingCheckoutRef else { return }
        events.send(.showCart)
        verifyPayment(reference: ref, result: result, force: true)
    }

    /// Fallback when the Hubtel page was closed without coming back through the deep link.
    func onAppResumed() {
        guard !checkoutPageOpen, let ref = session.pendingCheckoutRef else { return }
        switch checkoutState {
        case .awaitingPayment, .pending:
            verifyPayment(reference: ref, result: nil, force: false)
        default:
            break
        }
    }

    /// "Check payment" button on the pending banner.
    func checkPaymentAgain() {
        let ref: String?
        switch checkoutState {
        case let .pending(reference, _): ref = reference
        case let .awaitingPayment(reference): ref = reference
        default: ref = session.pendingCheckoutRef
        }
        guard let ref else { return }
        verifyPayment(reference: ref, result: nil, force: true)
    }

    func dismissCheckoutStatus() {
        switch checkoutState {
        case .verifying, .starting:
            return
        case .pending, .awaitingPayment:
            session.pendingCheckoutRef = nil
        default:
            break
        }
        checkoutState = .idle
    }

    private func verifyPayment(reference: String, result: String?, force: Bool) {
        if let running = verifyTask, !running.isCancelled {
            if !force { return }
            running.cancel()
        }
        guard session.accessToken != nil else {
            events.send(.requireLogin)
            return
        }
        let outcome = result?.lowercased()
        let cancelled = outcome == "cancel" || outcome == "cancelled"
        let reportedSuccess = outcome == "success"
        checkoutState = .verifying(reference: reference)

        verifyTask = Task {
            defer { if !Task.isCancelled { verifyTask = nil } }
            do {
                // Hubtel's callback to the backend can lag a few seconds behind the browser redirect.
                var attempts = reportedSuccess ? 4 : 1
                var status = try await authorized { [api] token in try await api.hubtelStatus(token: token, reference: reference) }
                attempts -= 1
                while !status.paid && status.status == "P" && attempts > 0 {
                    try await Task.sleep(nanoseconds: 2_500_000_000)
                    status = try await authorized { [api] token in try await api.hubtelStatus(token: token, reference: reference) }
                    attempts -= 1
                }
                if Task.isCancelled { return }

                if status.paid || status.status == "C" {
                    session.pendingCheckoutRef = nil
                    clearCart()
                    checkoutState = .paid(reference: reference, orderId: status.orderId)
                    events.send(.message("Payment received — thank you!"))
                } else if status.status == "F" {
                    session.pendingCheckoutRef = nil
                    checkoutState = .failed(message: "Payment failed. Your basket is saved — try again.")
                } else if cancelled {
                    session.pendingCheckoutRef = nil
                    checkoutState = .failed(message: "Payment cancelled. Your basket is saved.")
                } else {
                    checkoutState = .pending(
                        reference: reference,
                        message: "Payment pending. If you completed it on Hubtel, check again in a moment."
                    )
                }
            } catch {
                if Task.isCancelled { return }
                let e = APIError.from(error)
                if e.isUnauthorized {
                    expireSession()
                } else {
                    checkoutState = .pending(reference: reference, message: e.message)
                }
            }
        }
    }

    // MARK: - Settings

    func setMusicEnabled(_ enabled: Bool) {
        musicEnabled = enabled
        session.musicEnabled = enabled
        music.setUserEnabled(enabled)
    }
}
