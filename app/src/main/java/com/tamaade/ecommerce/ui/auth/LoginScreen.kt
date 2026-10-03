package com.tamaade.ecommerce.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.ui.components.AuthHeader
import com.tamaade.ecommerce.ui.components.AuthTextField
import com.tamaade.ecommerce.ui.components.PrimaryBrandButton
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Muted

/** Error "code" from api/user/otp/request/ for a number with no account. */
private const val NOT_REGISTERED = "not_registered"

/**
 * Sign in. Phone number + SMS code is the default; email + password stays available
 * for existing email accounts ("Sign in with email instead").
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    errorCode: String?,
    initialPhone: String,
    otpCooldownPhone: String?,
    otpCooldownUntil: Long,
    onBack: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    onRequestCode: (phone: String, onSent: (sentTo: String) -> Unit) -> Unit,
    onVerifyCode: (phone: String, code: String) -> Unit,
    onClearError: () -> Unit,
    onSignUp: (phone: String) -> Unit = {}
) {
    var useEmail by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable(initialPhone) { mutableStateOf(initialPhone) }
    /** Number the code was sent to (server's +233 form); null while on the phone step. */
    var codeSentTo by rememberSaveable { mutableStateOf<String?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

    fun edited() {
        localError = null
        onClearError()
    }

    val normalizedPhone = normalizeGhanaPhone(phone)
    val cooldown = rememberOtpCooldownSeconds(codeSentTo ?: normalizedPhone, otpCooldownPhone, otpCooldownUntil)

    fun sendCode() {
        val target = normalizedPhone
        if (target == null) {
            localError = "Enter a valid Ghana mobile number, e.g. 024 123 4567"
            return
        }
        onRequestCode(target) { sentTo ->
            codeSentTo = sentTo
            code = ""
            info = null
        }
    }

    BackHandler(enabled = !useEmail && codeSentTo != null) {
        codeSentTo = null
        code = ""
        edited()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        AuthHeader(
            title = "Welcome back",
            subtitle = when {
                useEmail -> "Sign in to checkout and track your Tamaade orders."
                codeSentTo != null -> "Enter the code from the SMS to sign in."
                else -> "Sign in with your phone number. We'll text you a code."
            }
        )

        val sentTo = codeSentTo
        when {
            useEmail -> {
                AuthTextField(
                    value = email,
                    onValueChange = { email = it; edited() },
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(Modifier.height(14.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it; edited() },
                    placeholder = "Password",
                    leadingIcon = Icons.Outlined.Lock,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordToggle(showPassword) { showPassword = !showPassword } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(16.dp))
                AuthErrorBlock(shownError)
                PrimaryBrandButton(
                    text = if (loading) "Signing in…" else "Sign In",
                    loading = loading,
                    enabled = !loading,
                    onClick = {
                        if (email.isBlank() || password.isBlank()) localError = "Enter email and password"
                        else onSubmit(email.trim(), password)
                    }
                )
            }

            sentTo != null -> OtpCodeStep(
                phone = sentTo,
                code = code,
                onCodeChange = { code = it; info = null; edited() },
                loading = loading,
                error = shownError,
                info = info,
                resendSeconds = cooldown,
                verifyLabel = "Sign In",
                onVerify = { typed -> onVerifyCode(sentTo, typed) },
                onResend = {
                    edited()
                    onRequestCode(sentTo) {
                        code = ""
                        info = "A new code is on its way."
                    }
                },
                onChangeNumber = {
                    codeSentTo = null
                    code = ""
                    edited()
                }
            )

            else -> {
                PhoneNumberField(value = phone, onValueChange = { phone = it; edited() })
                Spacer(Modifier.height(16.dp))
                AuthErrorBlock(
                    error = shownError,
                    actionLabel = if (localError == null && errorCode == NOT_REGISTERED) "Create an account" else null,
                    onAction = { edited(); onSignUp(normalizedPhone ?: phone.trim()) }
                )
                PrimaryBrandButton(
                    text = when {
                        loading -> "Sending code…"
                        cooldown > 0 -> "Send code (${cooldown}s)"
                        else -> "Send code"
                    },
                    loading = loading,
                    enabled = !loading && cooldown == 0,
                    onClick = ::sendCode
                )
            }
        }

        if (codeSentTo == null || useEmail) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { useEmail = !useEmail; edited() },
                enabled = !loading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (useEmail) "Sign in with phone number instead" else "Sign in with email instead",
                    color = BrandGreen
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", color = Muted)
            TextButton(onClick = { edited(); onSignUp(if (useEmail) "" else phone.trim()) }) {
                Text("Sign up", color = BrandGreen, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Continue as guest", color = Muted)
        }
    }
}

/**
 * Create an account (POST api/user/register/): names, email and/or phone, and a password.
 * The account is signed in straight away. A phone number lets the user sign in with an SMS
 * code later; an email lets them sign in with the password.
 */
@Composable
fun SignUpScreen(
    loading: Boolean,
    error: String?,
    /** Per-field server messages: first_name, last_name, email, phone_number, password, non_field_errors. */
    fieldErrors: Map<String, String>,
    initialPhone: String,
    onBack: () -> Unit,
    onSubmit: (firstName: String, lastName: String, email: String, phone: String, password: String) -> Unit,
    onClearError: () -> Unit,
    onSignIn: (phone: String) -> Unit,
    onOpenPrivacy: () -> Unit
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable(initialPhone) { mutableStateOf(initialPhone) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error
    val formError = fieldErrors["non_field_errors"]?.takeIf { localError == null && it != error }

    fun edited() {
        localError = null
        onClearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        AuthHeader(
            title = "Create your Tamaade account",
            subtitle = "Shop online in Ghana — Quality + Speed."
        )
        AuthErrorBlock(shownError)
        AuthErrorBlock(formError)

        AuthTextField(
            value = firstName,
            onValueChange = { firstName = it; edited() },
            placeholder = "First name",
            leadingIcon = Icons.Outlined.Person
        )
        FieldError(fieldErrors["first_name"])
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = lastName,
            onValueChange = { lastName = it; edited() },
            placeholder = "Last name",
            leadingIcon = Icons.Outlined.Person
        )
        FieldError(fieldErrors["last_name"])
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = email,
            onValueChange = { email = it; edited() },
            placeholder = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        FieldError(fieldErrors["email"])
        Spacer(Modifier.height(14.dp))
        PhoneNumberField(
            value = phone,
            onValueChange = { phone = it; edited() },
            placeholder = "Phone number (e.g. 024 123 4567)"
        )
        FieldError(fieldErrors["phone_number"])
        Spacer(Modifier.height(4.dp))
        Text(
            "Add an email to sign in with your password, or a phone number to sign in with an SMS code — or both.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it; edited() },
            placeholder = "Password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { PasswordToggle(showPassword) { showPassword = !showPassword } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        FieldError(fieldErrors["password"])
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; edited() },
            placeholder = "Confirm password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "At least $MIN_PASSWORD_LENGTH characters.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        PrimaryBrandButton(
            text = if (loading) "Creating account…" else "Create account",
            loading = loading,
            enabled = !loading,
            onClick = {
                val trimmedEmail = email.trim()
                val normalizedPhone = if (phone.isBlank()) "" else normalizeGhanaPhone(phone)
                localError = when {
                    firstName.isBlank() || lastName.isBlank() -> "Enter your first and last name"
                    trimmedEmail.isEmpty() && phone.isBlank() -> "Enter an email or a phone number."
                    trimmedEmail.isNotEmpty() && !EMAIL_PATTERN.matches(trimmedEmail) -> "Enter a valid email"
                    normalizedPhone == null -> "Enter a valid Ghana mobile number, e.g. 024 123 4567"
                    password.length < MIN_PASSWORD_LENGTH ->
                        "Password must be at least $MIN_PASSWORD_LENGTH characters"
                    password != confirmPassword -> "Passwords don't match"
                    else -> null
                }
                if (localError == null && normalizedPhone != null) {
                    onSubmit(firstName.trim(), lastName.trim(), trimmedEmail, normalizedPhone, password)
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "By signing up you agree to our ",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onOpenPrivacy) {
                Text("Privacy Policy", color = BrandGreen, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = Muted)
            TextButton(onClick = { edited(); onSignIn(phone.trim()) }) {
                Text("Sign in", color = BrandGreen, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back", color = Muted)
        }
    }
}

private const val MIN_PASSWORD_LENGTH = 8

private val EMAIL_PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (visible) "Hide password" else "Show password",
            tint = Muted
        )
    }
}
