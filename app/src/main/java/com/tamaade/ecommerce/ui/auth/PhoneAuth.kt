package com.tamaade.ecommerce.ui.auth

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.ui.components.PrimaryBrandButton
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Muted
import kotlinx.coroutines.delay

const val OTP_LENGTH = 6

private val GHANA_MOBILE = Regex("""^(\+?233|0)?[235]\d{8}$""")

/**
 * Light check of a Ghana mobile number (XXXXXXXXX, 0XXXXXXXXX, 233XXXXXXXXX or +233XXXXXXXXX, spaces and
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
    @Suppress("UNUSED_PARAMETER") placeholder: String = "",
    isError: Boolean = false,
    onDone: () -> Unit = {}
) {
    GhanaPhoneField(value = value, onValueChange = onValueChange, isError = isError, onDone = onDone)
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

/** Error card, plus an optional action (e.g. "Create an account" for an unknown number). */
@Composable
fun AuthErrorBlock(
    error: String?,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    title: String? = null
) {
    AuthNotice(message = error, title = title, actionLabel = actionLabel, onAction = onAction)
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
        Text("Code sent by SMS to", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatGhanaPhone(phone),
                color = Foreground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onChangeNumber, enabled = !loading) {
                Text("Change", color = BrandGreen, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        OtpBoxes(
            code = code,
            onCodeChange = { digits ->
                val completed = digits.length == OTP_LENGTH && digits != code
                onCodeChange(digits)
                if (completed && !loading) onVerify(digits)
            },
            isError = error != null,
            enabled = !loading
        )
        Spacer(Modifier.height(16.dp))
        AuthNotice(error)
        AuthNotice(if (error == null) info else null, kind = NoticeKind.Success)
        PrimaryBrandButton(
            text = if (loading) "Verifying…" else verifyLabel,
            loading = loading,
            enabled = !loading && code.length == OTP_LENGTH,
            onClick = { onVerify(code) }
        )
        Spacer(Modifier.height(4.dp))
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

/** +233241234567 → "+233 24 123 4567" (anything else is returned as is). */
fun formatGhanaPhone(phone: String): String {
    val m = Regex("""^\+233(\d{2})(\d{3})(\d{4})$""").find(phone) ?: return phone
    val (a, b, c) = m.destructured
    return "+233 $a $b $c"
}
