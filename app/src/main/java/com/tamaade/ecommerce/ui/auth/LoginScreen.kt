package com.tamaade.ecommerce.ui.auth

import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Border
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
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
    onSignUp: (phone: String) -> Unit = {},
    onForgotPassword: (phone: String) -> Unit = {}
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
            localError = "That number doesn't look right. Enter a Ghana mobile number, e.g. 24 123 4567."
            return
        }
        onRequestCode(target) { sentTo ->
            codeSentTo = sentTo
            code = ""
            info = null
        }
    }

    fun backToPhoneStep() {
        codeSentTo = null
        code = ""
        edited()
    }

    BackHandler(enabled = !useEmail && codeSentTo != null) { backToPhoneStep() }

    val sentTo = codeSentTo
    AuthScaffold(
        title = when {
            useEmail -> "Sign in with email"
            sentTo != null -> "Check your messages"
            else -> "Welcome back"
        },
        subtitle = when {
            useEmail -> "Use the email and password of your Tamaade account."
            sentTo != null -> "Enter the 6-digit code we just sent you."
            else -> null
        },
        onBack = { if (!useEmail && sentTo != null) backToPhoneStep() else onBack() },
        footer = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New to Tamaade? ", color = Muted)
                TextButton(onClick = { edited(); onSignUp(if (useEmail) "" else phone.trim()) }) {
                    Text("Create an account", color = BrandGreen, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack) {
                Text("Continue as guest", color = Muted)
            }
        }
    ) {
        when {
            useEmail -> {
                FieldLabel("Email")
                AuthTextField(
                    value = email,
                    onValueChange = { email = it; edited() },
                    placeholder = "you@example.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(14.dp))
                FieldLabel("Password")
                AuthTextField(
                    value = password,
                    onValueChange = { password = it; edited() },
                    placeholder = "Your password",
                    leadingIcon = Icons.Outlined.Lock,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordToggle(showPassword) { showPassword = !showPassword } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )
                TextButton(
                    onClick = { edited(); onForgotPassword("") },
                    enabled = !loading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot password?", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                }
                AuthNotice(
                    message = shownError,
                    title = if (localError == null && shownError != null &&
                        classifyError(shownError) == NoticeKind.Error) "Couldn't sign you in" else null
                )
                PrimaryBrandButton(
                    text = if (loading) "Signing in…" else "Sign in",
                    loading = loading,
                    enabled = !loading,
                    onClick = {
                        localError = when {
                            email.isBlank() && password.isBlank() -> "Enter your email and password."
                            email.isBlank() -> "Enter your email."
                            password.isBlank() -> "Enter your password."
                            else -> null
                        }
                        if (localError == null) onSubmit(email.trim(), password)
                    }
                )
                OrDivider()
                SwitchMethodButton(
                    text = "Continue with phone number",
                    icon = Icons.Outlined.PhoneAndroid,
                    enabled = !loading,
                    onClick = { useEmail = false; edited() }
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
                verifyLabel = "Sign in",
                onVerify = { typed -> onVerifyCode(sentTo, typed) },
                onResend = {
                    edited()
                    onRequestCode(sentTo) {
                        code = ""
                        info = "A new code is on its way."
                    }
                },
                onChangeNumber = ::backToPhoneStep
            )

            else -> {
                FieldLabel("Phone number")
                PhoneNumberField(
                    value = phone,
                    onValueChange = { phone = it; edited() },
                    isError = localError != null,
                    onDone = { if (!loading && cooldown == 0) sendCode() }
                )
                Spacer(Modifier.height(14.dp))
                val serverError = if (localError == null) error else null
                when {
                    serverError != null && classifyError(serverError) == NoticeKind.Unavailable -> AuthNotice(
                        message = "Signing in with a phone number isn't switched on yet. " +
                            "Use your email and password for now.",
                        kind = NoticeKind.Unavailable,
                        actionLabel = "Use email instead",
                        onAction = { useEmail = true; edited() }
                    )
                    serverError != null && errorCode == NOT_REGISTERED -> AuthNotice(
                        message = serverError,
                        title = "No account with this number",
                        actionLabel = "Create an account",
                        onAction = { edited(); onSignUp(normalizedPhone ?: phone.trim()) }
                    )
                    else -> AuthNotice(shownError)
                }
                PrimaryBrandButton(
                    text = when {
                        loading -> "Sending code…"
                        cooldown > 0 -> "Send code again in ${cooldown}s"
                        else -> "Send code"
                    },
                    loading = loading,
                    enabled = !loading && cooldown == 0,
                    onClick = ::sendCode
                )
                OrDivider()
                SwitchMethodButton(
                    text = "Continue with email",
                    icon = Icons.Outlined.Email,
                    enabled = !loading,
                    onClick = { useEmail = true; edited() }
                )
            }
        }
    }
}

/** Outlined button that switches between phone and email sign-in. */
@Composable
private fun SwitchMethodButton(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Foreground, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Foreground, fontWeight = FontWeight.SemiBold)
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

    AuthScaffold(
        title = "Create your account",
        subtitle = "Shop online in Ghana — Quality + Speed.",
        onBack = onBack,
        footer = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = Muted)
                TextButton(onClick = { edited(); onSignIn(phone.trim()) }) {
                    Text("Sign in", color = BrandGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("First name")
                AuthTextField(
                    value = firstName,
                    onValueChange = { firstName = it; edited() },
                    placeholder = "Ama",
                    leadingIcon = Icons.Outlined.Person
                )
                FieldError(fieldErrors["first_name"])
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("Last name")
                AuthTextField(
                    value = lastName,
                    onValueChange = { lastName = it; edited() },
                    placeholder = "Mensah",
                    leadingIcon = Icons.Outlined.Person
                )
                FieldError(fieldErrors["last_name"])
            }
        }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Phone number")
        PhoneNumberField(
            value = phone,
            onValueChange = { phone = it; edited() },
            isError = fieldErrors["phone_number"] != null
        )
        FieldError(fieldErrors["phone_number"])
        Spacer(Modifier.height(14.dp))
        FieldLabel("Email")
        AuthTextField(
            value = email,
            onValueChange = { email = it; edited() },
            placeholder = "you@example.com",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        FieldError(fieldErrors["email"])
        Spacer(Modifier.height(4.dp))
        Text(
            "Phone lets you sign in with an SMS code, email with your password. One of the two is enough.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(14.dp))
        FieldLabel("Password")
        AuthTextField(
            value = password,
            onValueChange = { password = it; edited() },
            placeholder = "At least $MIN_PASSWORD_LENGTH characters",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { PasswordToggle(showPassword) { showPassword = !showPassword } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        FieldError(fieldErrors["password"])
        Spacer(Modifier.height(14.dp))
        FieldLabel("Confirm password")
        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; edited() },
            placeholder = "Type it again",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(18.dp))
        // Errors sit right above the button, where the user is looking after tapping it.
        AuthNotice(
            message = shownError,
            title = if (localError == null && shownError != null &&
                classifyError(shownError) == NoticeKind.Error) "Couldn't create your account" else null
        )
        AuthNotice(formError)
        PrimaryBrandButton(
            text = if (loading) "Creating account…" else "Create account",
            loading = loading,
            enabled = !loading,
            onClick = {
                val trimmedEmail = email.trim()
                val normalizedPhone = if (phone.isBlank()) "" else normalizeGhanaPhone(phone)
                localError = when {
                    firstName.isBlank() || lastName.isBlank() -> "Enter your first and last name."
                    trimmedEmail.isEmpty() && phone.isBlank() -> "Add a phone number or an email so you can sign in."
                    trimmedEmail.isNotEmpty() && !EMAIL_PATTERN.matches(trimmedEmail) ->
                        "That email doesn't look right. Check it and try again."
                    normalizedPhone == null -> "That number doesn't look right. Enter a Ghana mobile number, e.g. 24 123 4567."
                    password.length < MIN_PASSWORD_LENGTH ->
                        "Your password needs at least $MIN_PASSWORD_LENGTH characters."
                    password != confirmPassword -> "The two passwords don't match."
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
