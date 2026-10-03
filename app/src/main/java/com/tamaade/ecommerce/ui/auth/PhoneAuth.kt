package com.tamaade.ecommerce.ui.auth

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.ui.components.AuthTextField
import com.tamaade.ecommerce.ui.components.PrimaryBrandButton
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Muted
import kotlinx.coroutines.delay

const val OTP_LENGTH = 6

private val GHANA_MOBILE = Regex("""^(\+?233|0)[235]\d{8}$""")

/**
 * Light check of a Ghana mobile number (0XXXXXXXXX, 233XXXXXXXXX or +233XXXXXXXXX, spaces and
 * dashes allowed). Returns it as +233XXXXXXXXX, or null when it doesn't look valid.
 * The server does the real validation.
 */
fun normalizeGhanaPhone(raw: String): String? {
    val compact = raw.trim().replace(Regex("""[\s\-()]"""), "")
    if (!GHANA_MOBILE.matches(compact)) return null
    return "+233" + compact.takeLast(9)
}

/**
 * Seconds left before another code can be requested for [phone], ticking down once a second.
 * The cooldown is tracked by the view model per phone number (from resend_in / retry_after).
 */
@Composable
fun rememberOtpCooldownSeconds(phone: String?, cooldownPhone: String?, cooldownUntil: Long): Int {
    val applies = phone != null && phone == cooldownPhone
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(applies, cooldownUntil) {
        now = SystemClock.elapsedRealtime()
        while (applies && now < cooldownUntil) {
            delay(1_000L - (cooldownUntil - now) % 1_000L)
            now = SystemClock.elapsedRealtime()
        }
    }
    if (!applies) return 0
    return ((cooldownUntil - now + 999) / 1_000).coerceAtLeast(0).toInt()
}

@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Phone number (e.g. 024 123 4567)"
) {
    AuthTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c in "+ -()" }) },
        placeholder = placeholder,
        leadingIcon = Icons.Outlined.Phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
    )
}

/** Server message for one field, shown right under it. */
@Composable
fun FieldError(message: String?) {
    if (message.isNullOrBlank()) return
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

/** Error text, plus an optional action under it (e.g. "Create an account" for an unknown number). */
@Composable
fun AuthErrorBlock(error: String?, actionLabel: String? = null, onAction: () -> Unit = {}) {
    if (error == null) return
    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    if (actionLabel != null) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(actionLabel, color = BrandGreen, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(8.dp))
}

/**
 * Step 2 of phone sign-in / sign-up: enter the 6-digit SMS code. Verifies automatically once
 * all digits are typed; [onVerify] is also wired to the button for retries.
 */
@Composable
fun OtpCodeStep(
    phone: String,
    code: String,
    onCodeChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    info: String?,
    resendSeconds: Int,
    verifyLabel: String,
    onVerify: (code: String) -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("We sent a 6-digit code to", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                phone,
                color = Foreground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onChangeNumber, enabled = !loading) {
                Text("Change number", color = BrandGreen)
            }
        }
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = code,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(OTP_LENGTH)
                val completed = digits.length == OTP_LENGTH && digits != code
                onCodeChange(digits)
                if (completed && !loading) onVerify(digits)
            },
            placeholder = "6-digit code",
            leadingIcon = Icons.Outlined.Sms,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(16.dp))
        AuthErrorBlock(error)
        if (error == null && info != null) {
            Text(info, color = BrandGreen, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        PrimaryBrandButton(
            text = if (loading) "Verifying…" else verifyLabel,
            loading = loading,
            enabled = !loading && code.length == OTP_LENGTH,
            onClick = { onVerify(code) }
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Didn't get it? ", color = Muted)
            TextButton(onClick = onResend, enabled = !loading && resendSeconds == 0) {
                Text(
                    if (resendSeconds > 0) "Resend in ${resendSeconds}s" else "Resend code",
                    color = if (resendSeconds > 0) Muted else BrandGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
