package com.tamaade.ecommerce.ui.auth

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

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    onClearError: () -> Unit,
    onSignUp: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        AuthHeader(
            title = "Welcome back",
            subtitle = "Sign in to checkout and track your Tamaade orders."
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it; localError = null; onClearError() },
            placeholder = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it; localError = null; onClearError() },
            placeholder = "Password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { PasswordToggle(showPassword) { showPassword = !showPassword } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(16.dp))
        shownError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        PrimaryBrandButton(
            text = if (loading) "Signing in…" else "Sign In",
            loading = loading,
            enabled = !loading,
            onClick = {
                if (email.isBlank() || password.isBlank()) localError = "Enter email and password"
                else onSubmit(email.trim(), password)
            }
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", color = Muted)
            TextButton(onClick = onSignUp) {
                Text("Sign up", color = BrandGreen, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Continue as guest", color = Muted)
        }
    }
}

@Composable
fun SignUpScreen(
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (email: String, password: String, confirmPassword: String, firstName: String, lastName: String) -> Unit,
    onClearError: () -> Unit,
    onSignIn: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

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
        AuthTextField(
            value = email,
            onValueChange = { email = it; edited() },
            placeholder = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = fullName,
            onValueChange = { fullName = it; edited() },
            placeholder = "Full name",
            leadingIcon = Icons.Outlined.Person
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
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; edited() },
            placeholder = "Confirm password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(16.dp))
        shownError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        PrimaryBrandButton(
            text = if (loading) "Creating account…" else "Sign Up",
            loading = loading,
            enabled = !loading,
            onClick = {
                val parts = fullName.trim().split(Regex("\\s+"), limit = 2)
                localError = when {
                    email.isBlank() || fullName.isBlank() || password.isBlank() -> "Fill in all fields"
                    password != confirmPassword -> "Passwords don't match"
                    else -> null
                }
                if (localError == null) {
                    onSubmit(
                        email.trim(),
                        password,
                        confirmPassword,
                        parts.getOrElse(0) { "" },
                        parts.getOrElse(1) { "" }
                    )
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
            TextButton(onClick = onSignIn) {
                Text("Sign in", color = BrandGreen, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back", color = Muted)
        }
    }
}

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
