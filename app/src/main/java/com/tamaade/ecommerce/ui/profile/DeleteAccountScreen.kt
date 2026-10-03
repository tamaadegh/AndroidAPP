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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tamaade.ecommerce.ui.components.AuthTextField
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Muted
import com.tamaade.ecommerce.ui.theme.PageBackground
import com.tamaade.ecommerce.ui.theme.White

/**
 * Google Play account deletion: POST api/user/delete-account/ with the user's password.
 */
@Composable
fun DeleteAccountScreen(
    email: String?,
    phoneNumber: String?,
    deleting: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onConfirm: (password: String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

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
            IconButton(onClick = onBack, enabled = !deleting) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = "Delete account",
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
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Permanently delete your Tamaade account?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Foreground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildString {
                    append("This erases your account")
                    val contact = email?.takeIf { it.isNotBlank() } ?: phoneNumber?.takeIf { it.isNotBlank() }
                    if (contact != null) append(" (").append(contact).append(")")
                    append(" and the personal data linked to it, including your profile, ")
                    append("saved basket and delivery details. You will be signed out on this device. ")
                    append("This cannot be undone.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Enter your password to confirm",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            AuthTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                    onClearError()
                },
                placeholder = "Password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            shownError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (password.isBlank()) localError = "Enter your password"
                    else onConfirm(password)
                },
                enabled = !deleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = White,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    disabledContentColor = White
                )
            ) {
                if (deleting) {
                    CircularProgressIndicator(
                        color = White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (deleting) "Deleting…" else "Delete my account",
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = onBack,
                enabled = !deleting,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Keep my account", color = Muted)
            }
        }
    }
}
