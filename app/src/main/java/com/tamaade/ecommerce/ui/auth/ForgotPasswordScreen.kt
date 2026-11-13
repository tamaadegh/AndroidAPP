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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sms
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.ui.components.AuthHeader
import com.tamaade.ecommerce.ui.components.AuthTextField
import com.tamaade.ecommerce.ui.components.PrimaryBrandButton
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Muted

private const val MIN_RESET_PASSWORD_LENGTH = 8

/**
 * Forgotten password, by SMS: phone number -> 6-digit code + new password. On success the
 * server signs the user in (POST api/user/password/reset/otp/confirm/).
 */
@Composable
fun ForgotPasswordScreen(
    loading: Boolean,
    error: String?,
    /** Per-field server messages: phone_number, code, new_password. */
    fieldErrors: Map<String, String>,
    initialPhone: String,
    otpCooldownPhone: String?,
    otpCooldownUntil: Long,
    onRequestCode: (phone: String, onSent: (sentTo: String) -> Unit) -> Unit,
    onReset: (phone: String, code: String, newPassword: String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var phone by rememberSaveable(initialPhone) { mutableStateOf(initialPhone) }
    var codeSentTo by rememberSaveable { mutableStateOf<String?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

    fun edited() {
        localError = null
        info = null
        onClearError()
    }

    val normalizedPhone = normalizeGhanaPhone(phone)
    val cooldown = rememberOtpCooldownSeconds(codeSentTo ?: normalizedPhone, otpCooldownPhone, otpCooldownUntil)

    fun backToPhoneStep() {
        codeSentTo = null
        code = ""
        edited()
    }

    BackHandler(enabled = codeSentTo != null) { backToPhoneStep() }

    val sentTo = codeSentTo
    AuthScaffold(
        title = "Reset your password",
        subtitle = if (sentTo == null) {
            "Enter the phone number on your account. We'll text you a code."
        } else {
            "Enter the code from the SMS and choose a new password."
        },
        onBack = { if (sentTo != null) backToPhoneStep() else onBack() },
        backEnabled = !loading
    ) {
        if (sentTo == null) {
            FieldLabel("Phone number")
            PhoneNumberField(
                value = phone,
                onValueChange = { phone = it; edited() },
                isError = localError != null || fieldErrors["phone_number"] != null
            )
            FieldError(fieldErrors["phone_number"])
            Spacer(Modifier.height(14.dp))
            AuthErrorBlock(shownError)
            PrimaryBrandButton(
                text = when {
                    loading -> "Sending code…"
                    cooldown > 0 -> "Send code (${cooldown}s)"
                    else -> "Send code"
                },
                loading = loading,
                enabled = !loading && cooldown == 0,
                onClick = {
                    val target = normalizedPhone
                    if (target == null) {
                        localError = "That number doesn't look right. Enter a Ghana mobile number, e.g. 24 123 4567."
                    } else {
                        onRequestCode(target) { sent ->
                            codeSentTo = sent
                            code = ""
                        }
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
            AuthNotice(
                message = "No phone number on your account? Sign in with your email and password, " +
                    "then add one in Profile → Edit details.",
                kind = NoticeKind.Info
            )
        } else {
            Text("Code sent by SMS to", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatGhanaPhone(sentTo), color = Foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = ::backToPhoneStep, enabled = !loading) {
                    Text("Change", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            OtpBoxes(
                code = code,
                onCodeChange = { code = it; edited() },
                isError = fieldErrors["code"] != null,
                enabled = !loading
            )
            FieldError(fieldErrors["code"])
            Spacer(Modifier.height(18.dp))
            FieldLabel("New password")
            AuthTextField(
                value = password,
                onValueChange = { password = it; edited() },
                placeholder = "New password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Muted
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            FieldError(fieldErrors["new_password"])
            Spacer(Modifier.height(14.dp))
            FieldLabel("Confirm new password")
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; edited() },
                placeholder = "Confirm new password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )
            Spacer(Modifier.height(4.dp))
            Text("At least $MIN_RESET_PASSWORD_LENGTH characters.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            AuthErrorBlock(shownError)
            AuthNotice(if (shownError == null) info else null, kind = NoticeKind.Success)
            PrimaryBrandButton(
                text = if (loading) "Saving…" else "Reset password",
                loading = loading,
                enabled = !loading,
                onClick = {
                    localError = when {
                        code.length != OTP_LENGTH -> "Enter the 6-digit code we sent you."
                        password.length < MIN_RESET_PASSWORD_LENGTH ->
                            "Your new password needs at least $MIN_RESET_PASSWORD_LENGTH characters."
                        password != confirmPassword -> "The two passwords don't match."
                        else -> null
                    }
                    if (localError == null) onReset(sentTo, code, password)
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Didn't get it? ", color = Muted)
                TextButton(
                    onClick = {
                        edited()
                        onRequestCode(sentTo) {
                            code = ""
                            info = "A new code is on its way."
                        }
                    },
                    enabled = !loading && cooldown == 0
                ) {
                    Text(
                        if (cooldown > 0) "Resend in ${cooldown}s" else "Resend code",
                        color = if (cooldown > 0) Muted else BrandGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
