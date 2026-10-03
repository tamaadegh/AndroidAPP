package com.tamaade.ecommerce.data

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tamaade.ecommerce.data.api.ApiException
import com.tamaade.ecommerce.data.api.AuthResult
import com.tamaade.ecommerce.data.api.OtpRequestResult
import com.tamaade.ecommerce.data.api.TamaadeApi
import com.tamaade.ecommerce.data.model.CartLine
import com.tamaade.ecommerce.data.model.CheckoutState
import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.data.model.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** One-off things the UI should do in response to the store (snackbars, navigation, opening the browser). */
sealed interface StoreEvent {
    data class Message(val text: String) : StoreEvent
    data class OpenUrl(val url: String) : StoreEvent
    data object RequireLogin : StoreEvent
    data object ShowCart : StoreEvent
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val session = SessionStore(application)
    val music = BackgroundMusicPlayer()

    private val eventChannel = Channel<StoreEvent>(Channel.BUFFERED)
    val events: Flow<StoreEvent> = eventChannel.receiveAsFlow()

    var cartLines by mutableStateOf(session.loadCart())
        private set

    var user by mutableStateOf(session.loadUser())
        private set

    var authLoading by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    /** The "code" of the last auth error (e.g. "not_registered"). */
    var authErrorCode by mutableStateOf<String?>(null)
        private set

    /** Per-field messages of the last auth error (e.g. "password" for a too-weak sign-up password). */
    var authFieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** Phone number (+233 form) whose SMS code can't be re-sent before [otpCooldownUntil]. */
    var otpCooldownPhone by mutableStateOf<String?>(null)
        private set

    /** [SystemClock.elapsedRealtime] at which another code may be requested for [otpCooldownPhone]. */
    var otpCooldownUntil by mutableStateOf(0L)
        private set

    var checkoutState by mutableStateOf<CheckoutState>(
        session.pendingCheckoutRef?.let { CheckoutState.AwaitingPayment(it) } ?: CheckoutState.Idle
    )
        private set

    var musicEnabled by mutableStateOf(session.musicEnabled)
        private set

    var deletingAccount by mutableStateOf(false)
        private set

    var savingProfile by mutableStateOf(false)
        private set

    var profileError by mutableStateOf<String?>(null)
        private set

    /** Per-field messages from PATCH api/user/ (first_name, last_name, email, phone_number). */
    var profileFieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var deleteAccountError by mutableStateOf<String?>(null)
        private set

    private var verifyJob: Job? = null

    val cartCount: Int
        get() = cartLines.sumOf { it.quantity }

    val cartTotal: Double
        get() = cartLines.sumOf { it.lineTotal }

    init {
        music.setUserEnabled(musicEnabled)
        viewModelScope.launch {
            try {
                val config = TamaadeApi.backgroundMusic()
                music.setTrack(if (config.enabled) config.track else null)
            } catch (_: ApiException) {
                // No music when the backend is unreachable or the endpoint isn't deployed yet.
            }
        }
    }

    // region Cart

    fun addToCart(product: Product, qty: Int = 1) {
        val existing = cartLines.find { it.product.id == product.id }
        updateCart(
            if (existing != null) {
                cartLines.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + qty) else it
                }
            } else {
                cartLines + CartLine(product, qty)
            }
        )
    }

    fun setQuantity(productId: Int, qty: Int) {
        updateCart(
            if (qty <= 0) {
                cartLines.filterNot { it.product.id == productId }
            } else {
                cartLines.map {
                    if (it.product.id == productId) it.copy(quantity = qty) else it
                }
            }
        )
    }

    fun removeFromCart(productId: Int) {
        updateCart(cartLines.filterNot { it.product.id == productId })
    }

    fun clearCart() {
        updateCart(emptyList())
    }

    /**
     * Re-syncs the saved basket with the live catalog: drops products that no longer exist
     * or are out of stock, caps quantities to stock and refreshes names/prices/images.
     */
    fun syncCartWithCatalog(products: List<Product>) {
        if (cartLines.isEmpty()) return
        val byId = products.associateBy { it.id }
        val synced = cartLines.mapNotNull { line ->
            val fresh = byId[line.product.id] ?: return@mapNotNull null
            if (fresh.quantity <= 0) return@mapNotNull null
            CartLine(fresh, line.quantity.coerceAtMost(fresh.quantity))
        }
        if (synced == cartLines) return
        val removed = synced.size < cartLines.size
        val priceChanged = synced.any { line ->
            cartLines.find { it.product.id == line.product.id }?.product?.price != line.product.price
        }
        cartLines = synced
        session.saveCart(synced)
        if (removed || priceChanged) {
            eventChannel.trySend(
                StoreEvent.Message(
                    if (removed) "Some basket items are no longer available and were removed."
                    else "Basket prices were updated."
                )
            )
        }
    }

    private fun updateCart(lines: List<CartLine>) {
        cartLines = lines
        session.saveCart(lines)
        // Editing the basket after a failed/finished attempt dismisses the old status banner.
        if (checkoutState is CheckoutState.Failed || checkoutState is CheckoutState.Paid) {
            checkoutState = CheckoutState.Idle
        }
    }

    // endregion

    // region Auth

    fun clearAuthError() {
        authError = null
        authErrorCode = null
        authFieldErrors = emptyMap()
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        signIn({ onSuccess() }) { TamaadeApi.login(email, password) }
    }

    /** POST api/user/otp/request/ — texts a 6-digit code to [phone] (+233 form). */
    fun requestOtp(phone: String, onSent: (OtpRequestResult) -> Unit) {
        if (authLoading) return
        authLoading = true
        clearAuthError()
        viewModelScope.launch {
            try {
                val result = TamaadeApi.requestOtp(phone)
                startOtpCooldown(phone, result.resendIn)
                if (result.phoneNumber != phone) startOtpCooldown(result.phoneNumber, result.resendIn)
                onSent(result)
            } catch (e: ApiException) {
                onAuthFailed(e, phone)
            } finally {
                authLoading = false
            }
        }
    }

    /** POST api/user/otp/verify/ — signs in with the SMS code. */
    fun verifyOtp(phone: String, code: String, onSuccess: () -> Unit) {
        signIn({ onSuccess() }, phone) { TamaadeApi.verifyOtp(phone, code) }
    }

    /**
     * POST api/user/register/ — creates the account (email and/or phone + password) and
     * signs it in. Per-field server errors end up in [authFieldErrors].
     */
    fun register(
        firstName: String,
        lastName: String,
        password: String,
        email: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        signIn({ onSuccess() }) {
            TamaadeApi.register(firstName, lastName, password, email, phone)
        }
    }

    private fun signIn(onSuccess: (AuthResult) -> Unit, phone: String? = null, call: suspend () -> AuthResult) {
        if (authLoading) return
        authLoading = true
        clearAuthError()
        viewModelScope.launch {
            try {
                val result = call()
                session.saveSession(result.accessToken, result.user)
                user = result.user
                otpCooldownPhone = null
                onSuccess(result)
            } catch (e: ApiException) {
                onAuthFailed(e, phone)
            } finally {
                authLoading = false
            }
        }
    }

    private fun onAuthFailed(e: ApiException, phone: String?) {
        authError = e.message ?: "Something went wrong. Please try again."
        authErrorCode = e.code
        authFieldErrors = e.fieldErrors
        val retryAfter = e.retryAfter
        if (phone != null && e.status == 429 && retryAfter != null) startOtpCooldown(phone, retryAfter)
    }

    private fun startOtpCooldown(phone: String, seconds: Int) {
        otpCooldownPhone = phone
        otpCooldownUntil = SystemClock.elapsedRealtime() + seconds.coerceAtLeast(0) * 1_000L
    }

    fun logout() {
        verifyJob?.cancel()
        session.clearSession()
        user = null
        checkoutState = CheckoutState.Idle
    }

    fun clearProfileError() {
        profileError = null
        profileFieldErrors = emptyMap()
    }

    /**
     * PATCH api/user/ with the edited names and contact details ([email]/[phone] "" removes it);
     * on success the stored user is replaced with the server's copy.
     */
    fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        onSaved: () -> Unit
    ) {
        val token = session.accessToken
        if (token == null) {
            eventChannel.trySend(StoreEvent.RequireLogin)
            return
        }
        if (savingProfile) return
        savingProfile = true
        clearProfileError()
        viewModelScope.launch {
            try {
                val updated = TamaadeApi.updateProfile(token, firstName, lastName, email, phone)
                session.saveUser(updated)
                user = updated
                onSaved()
            } catch (e: ApiException) {
                if (e.isUnauthorized) {
                    expireSession()
                } else {
                    profileError = e.message ?: "Couldn't save your details. Please try again."
                    profileFieldErrors = e.fieldErrors
                }
            } finally {
                savingProfile = false
            }
        }
    }

    fun clearDeleteAccountError() {
        deleteAccountError = null
    }

    /**
     * POST api/user/delete-account/; on success wipes the session, basket and pending checkout.
     */
    fun deleteAccount(password: String, onDeleted: (message: String) -> Unit) {
        val token = session.accessToken
        if (token == null) {
            eventChannel.trySend(StoreEvent.RequireLogin)
            return
        }
        if (deletingAccount) return
        deletingAccount = true
        deleteAccountError = null
        viewModelScope.launch {
            try {
                val message = TamaadeApi.deleteAccount(token, password)
                logout()
                cartLines = emptyList()
                session.saveCart(emptyList())
                onDeleted(message)
            } catch (e: ApiException) {
                if (e.isUnauthorized) expireSession()
                else deleteAccountError = e.message ?: "Couldn't delete your account. Please try again."
            } finally {
                deletingAccount = false
            }
        }
    }

    private fun expireSession() {
        logout()
        eventChannel.trySend(StoreEvent.Message("Your session has expired. Please sign in again."))
        eventChannel.trySend(StoreEvent.RequireLogin)
    }

    // endregion

    // region Hubtel checkout

    fun startCheckout() {
        val token = session.accessToken
        if (user == null || token == null) {
            eventChannel.trySend(StoreEvent.RequireLogin)
            return
        }
        if (cartLines.isEmpty() || checkoutState is CheckoutState.Starting) return
        verifyJob?.cancel()
        checkoutState = CheckoutState.Starting
        viewModelScope.launch {
            try {
                val checkout = TamaadeApi.startHubtelCheckout(token, cartLines)
                session.pendingCheckoutRef = checkout.clientReference
                checkoutState = CheckoutState.AwaitingPayment(checkout.clientReference)
                eventChannel.send(StoreEvent.OpenUrl(checkout.checkoutUrl))
            } catch (e: ApiException) {
                if (e.isUnauthorized) {
                    expireSession()
                } else {
                    checkoutState = CheckoutState.Failed(e.message ?: "Checkout failed. Please try again.")
                }
            }
        }
    }

    /** Deep link tamaade://checkout/result?ref=...&result=success|cancel from the backend return page. */
    fun onCheckoutReturn(reference: String?, result: String?) {
        val ref = reference?.takeIf { it.isNotBlank() } ?: session.pendingCheckoutRef ?: return
        eventChannel.trySend(StoreEvent.ShowCart)
        verifyPayment(ref, result, force = true)
    }

    /** Fallback when the browser didn't bounce back through the deep link. */
    fun onAppResumed() {
        val ref = session.pendingCheckoutRef ?: return
        if (checkoutState is CheckoutState.AwaitingPayment || checkoutState is CheckoutState.Pending) {
            verifyPayment(ref, result = null, force = false)
        }
    }

    /** "Check payment" button on the pending banner. */
    fun checkPaymentAgain() {
        val ref = when (val state = checkoutState) {
            is CheckoutState.Pending -> state.reference
            is CheckoutState.AwaitingPayment -> state.reference
            else -> session.pendingCheckoutRef
        } ?: return
        verifyPayment(ref, result = null, force = true)
    }

    fun dismissCheckoutStatus() {
        if (checkoutState is CheckoutState.Verifying || checkoutState is CheckoutState.Starting) return
        if (checkoutState is CheckoutState.Pending || checkoutState is CheckoutState.AwaitingPayment) {
            session.pendingCheckoutRef = null
        }
        checkoutState = CheckoutState.Idle
    }

    private fun verifyPayment(reference: String, result: String?, force: Boolean) {
        if (verifyJob?.isActive == true) {
            if (!force) return
            verifyJob?.cancel()
        }
        val token = session.accessToken
        if (token == null) {
            eventChannel.trySend(StoreEvent.RequireLogin)
            return
        }
        val cancelled = result.equals("cancel", ignoreCase = true) ||
            result.equals("cancelled", ignoreCase = true)
        val reportedSuccess = result.equals("success", ignoreCase = true)
        checkoutState = CheckoutState.Verifying(reference)

        verifyJob = viewModelScope.launch {
            try {
                // Hubtel's callback to the backend can lag a few seconds behind the browser redirect.
                var attempts = if (reportedSuccess) 4 else 1
                var status = TamaadeApi.hubtelStatus(token, reference)
                while (!status.paid && status.status == "P" && --attempts > 0) {
                    delay(2_500)
                    status = TamaadeApi.hubtelStatus(token, reference)
                }

                when {
                    status.paid || status.status == "C" -> {
                        session.pendingCheckoutRef = null
                        clearCart()
                        checkoutState = CheckoutState.Paid(reference, status.orderId)
                        eventChannel.send(StoreEvent.Message("Payment received — thank you!"))
                    }
                    status.status == "F" -> {
                        session.pendingCheckoutRef = null
                        checkoutState = CheckoutState.Failed("Payment failed. Your basket is saved — try again.")
                    }
                    cancelled -> {
                        session.pendingCheckoutRef = null
                        checkoutState = CheckoutState.Failed("Payment cancelled. Your basket is saved.")
                    }
                    else -> {
                        checkoutState = CheckoutState.Pending(
                            reference,
                            "Payment pending. If you completed it on Hubtel, check again in a moment."
                        )
                    }
                }
            } catch (e: ApiException) {
                if (e.isUnauthorized) {
                    expireSession()
                } else {
                    checkoutState = CheckoutState.Pending(
                        reference,
                        e.message ?: "Couldn't check your payment. Please try again."
                    )
                }
            }
        }
    }

    // endregion

    // region Settings

    fun updateMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        session.musicEnabled = enabled
        music.setUserEnabled(enabled)
    }

    // endregion

    override fun onCleared() {
        music.release()
        super.onCleared()
    }
}
