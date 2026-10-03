package com.tamaade.ecommerce.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.data.model.UserSession
import com.tamaade.ecommerce.ui.auth.AuthErrorBlock
import com.tamaade.ecommerce.ui.auth.FieldError
import com.tamaade.ecommerce.ui.auth.PhoneNumberField
import com.tamaade.ecommerce.ui.auth.normalizeGhanaPhone
import com.tamaade.ecommerce.ui.components.AuthTextField
import com.tamaade.ecommerce.ui.components.PrimaryBrandButton
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Muted
import com.tamaade.ecommerce.ui.theme.PageBackground

private val EMAIL_PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/**
 * "Edit my details": PATCH api/user/ with names, email and phone. Adding a phone turns on
 * SMS-code sign-in; adding an email turns on email + password sign-in. At least one must remain.
 */
@Composable
fun EditProfileScreen(
    user: UserSession,
    saving: Boolean,
    error: String?,
    /** Per-field server messages: first_name, last_name, email, phone_number. */
    fieldErrors: Map<String, String>,
    onClearError: () -> Unit,
    onSave: (firstName: String, lastName: String, email: String, phone: String) -> Unit,
    onBack: () -> Unit
) {
    var firstName by rememberSaveable { mutableStateOf(user.firstName) }
    var lastName by rememberSaveable { mutableStateOf(user.lastName) }
    var email by rememberSaveable { mutableStateOf(user.email) }
    var phone by rememberSaveable { mutableStateOf(user.phoneNumber) }
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
            .background(PageBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandGreen)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !saving) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = "Edit my details",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How you can sign in",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SignInMethodRow(
                        label = "SMS code sign-in",
                        on = user.phoneNumber.isNotBlank(),
                        hint = "add a phone number"
                    )
                    Spacer(Modifier.height(6.dp))
                    SignInMethodRow(
                        label = "Email + password sign-in",
                        on = user.email.isNotBlank(),
                        hint = "add an email"
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

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
            PhoneNumberField(value = phone, onValueChange = { phone = it; edited() })
            FieldError(fieldErrors["phone_number"])
            Spacer(Modifier.height(4.dp))
            Text(
                "Leave a field empty to remove it. Keep at least an email or a phone number.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(20.dp))
            PrimaryBrandButton(
                text = if (saving) "Saving…" else "Save",
                loading = saving,
                enabled = !saving,
                onClick = {
                    val trimmedEmail = email.trim()
                    val normalizedPhone = if (phone.isBlank()) "" else normalizeGhanaPhone(phone)
                    localError = when {
                        firstName.isBlank() || lastName.isBlank() -> "Enter your first and last name"
                        trimmedEmail.isEmpty() && phone.isBlank() -> "Enter an email or a phone number."
                        trimmedEmail.isNotEmpty() && !EMAIL_PATTERN.matches(trimmedEmail) -> "Enter a valid email"
                        normalizedPhone == null -> "Enter a valid Ghana mobile number, e.g. 024 123 4567"
                        else -> null
                    }
                    if (localError == null && normalizedPhone != null) {
                        onSave(firstName.trim(), lastName.trim(), trimmedEmail, normalizedPhone)
                    }
                }
            )
            TextButton(
                onClick = onBack,
                enabled = !saving,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cancel", color = Muted)
            }
        }
    }
}

@Composable
private fun SignInMethodRow(label: String, on: Boolean, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (on) Icons.Outlined.CheckCircle else Icons.Outlined.RemoveCircleOutline,
            contentDescription = null,
            tint = if (on) BrandGreen else Muted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = if (on) "$label: on" else "$label: off ($hint)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (on) MaterialTheme.colorScheme.onSurface else Muted
        )
    }
}
