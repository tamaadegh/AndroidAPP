package com.tamaade.ecommerce.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamaade.ecommerce.data.api.TamaadeApi
import com.tamaade.ecommerce.ui.components.TamaadeWordmark
import com.tamaade.ecommerce.ui.theme.Border
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.Foreground
import com.tamaade.ecommerce.ui.theme.Muted
import com.tamaade.ecommerce.ui.theme.PageBackground
import com.tamaade.ecommerce.ui.theme.White

/**
 * Shared frame for Sign in / Sign up / Reset password: green brand header with a back arrow
 * (these screens have no bottom bar), then the form on a white card that overlaps the header.
 * The screen handles its own system-bar and keyboard insets.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandGreen, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .statusBarsPadding()
                .padding(bottom = 44.dp)
        ) {
            IconButton(
                onClick = onBack,
                enabled = backEnabled,
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp)
                    .size(44.dp)
                    .background(White.copy(alpha = 0.14f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = White)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TamaadeWordmark(height = 30.dp)
                Spacer(Modifier.height(20.dp))
                Text(
                    title,
                    color = White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        subtitle,
                        color = White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Surface(
            color = White,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-28).dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = footer
        )
    }
}

/** Small label above a field. */
@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        color = Foreground,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

enum class NoticeKind { Error, Offline, Unavailable, Wait, Server, Success, Info }

/** What kind of message an API error is, from the texts TamaadeApi produces. */
fun classifyError(message: String): NoticeKind = when {
    message == TamaadeApi.OFFLINE_MESSAGE -> NoticeKind.Offline
    message == TamaadeApi.UNAVAILABLE_MESSAGE -> NoticeKind.Unavailable
    message.startsWith(TamaadeApi.SERVER_ERROR_PREFIX) -> NoticeKind.Server
    message.contains("too many", ignoreCase = true) ||
        message.contains("try again in", ignoreCase = true) -> NoticeKind.Wait
    else -> NoticeKind.Error
}

private data class NoticeStyle(val icon: ImageVector, val tint: Color, val background: Color, val title: String?)

private fun styleFor(kind: NoticeKind): NoticeStyle = when (kind) {
    NoticeKind.Error -> NoticeStyle(Icons.Outlined.ErrorOutline, Color(0xFFB3261E), Color(0xFFFDECEA), null)
    NoticeKind.Offline -> NoticeStyle(Icons.Outlined.WifiOff, Color(0xFF8A5A00), Color(0xFFFFF4E0), "You're offline")
    NoticeKind.Unavailable -> NoticeStyle(Icons.Outlined.CloudOff, Color(0xFF2D54A4), Color(0xFFEAF0FB), "Not available yet")
    NoticeKind.Wait -> NoticeStyle(Icons.Outlined.HourglassTop, Color(0xFF8A5A00), Color(0xFFFFF4E0), "Please wait a moment")
    NoticeKind.Server -> NoticeStyle(Icons.Outlined.CloudOff, Color(0xFFB3261E), Color(0xFFFDECEA), "Our server had a problem")
    NoticeKind.Success -> NoticeStyle(Icons.Outlined.CheckCircle, BrandGreen, Color(0xFFE8F1EC), null)
    NoticeKind.Info -> NoticeStyle(Icons.Outlined.Info, BrandGreen, Color(0xFFE8F1EC), null)
}

/**
 * Message card used for every auth error and confirmation: icon, optional title, text and an
 * optional action (e.g. "Create an account", "Use email instead").
 */
@Composable
fun AuthNotice(
    message: String?,
    kind: NoticeKind = message?.let(::classifyError) ?: NoticeKind.Error,
    title: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    AnimatedVisibility(visible = message != null, enter = fadeIn() + expandVertically()) {
        if (message == null) return@AnimatedVisibility
        val style = styleFor(kind)
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(style.background, RoundedCornerShape(12.dp))
                    .border(1.dp, style.tint.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(style.icon, contentDescription = null, tint = style.tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val heading = title ?: style.title
                    if (heading != null) {
                        Text(heading, color = style.tint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(message, color = Foreground.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    if (actionLabel != null) {
                        TextButton(
                            onClick = onAction,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(actionLabel, color = style.tint, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ghana mobile number: fixed "🇬🇭 +233" prefix, the 9 local digits shown grouped as 24 123 4567.
 * A leading 0 or 233 typed out of habit is dropped (see ghanaLocalDigits).
 */
@Composable
fun GhanaPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {}
) {
    OutlinedTextField(
        value = ghanaLocalDigits(value),
        onValueChange = { input -> onValueChange(ghanaLocalDigits(input)) },
        placeholder = { Text("24 123 4567", color = Muted) },
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 6.dp)
            ) {
                Text("🇬🇭", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text("+233", color = Foreground, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Border)
                )
            }
        },
        singleLine = true,
        isError = isError,
        visualTransformation = GhanaPhoneTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }, onGo = { onDone() }),
        textStyle = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.5.sp),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandGreen,
            unfocusedBorderColor = Border,
            focusedContainerColor = White,
            unfocusedContainerColor = White
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * The 9 local digits after +233: drops what the prefix already says, so typing or pasting
 * "024 123 4567", "+233 24 123 4567" or "233241234567" all give "241234567".
 */
fun ghanaLocalDigits(raw: String): String {
    var digits = raw.filter { it.isDigit() }
    if (digits.startsWith("233") && digits.length > 9) digits = digits.drop(3)
    digits = digits.trimStart('0')
    return digits.take(9)
}

/** Inserts spaces: 241234567 → "24 123 4567". */
private object GhanaPhoneTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val breaks = listOf(2, 5) // a space goes before these positions
        val out = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i in breaks) out.append(' ')
            out.append(c)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + breaks.count { offset > it }
            override fun transformedToOriginal(offset: Int): Int {
                var original = offset
                breaks.forEachIndexed { index, b -> if (offset > b + index) original-- }
                return original.coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

/** Six boxes for the SMS code; the whole row is one text field so paste and autofill work. */
@Composable
fun OtpBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    isError: Boolean,
    enabled: Boolean = true
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = code,
        onValueChange = { input -> onCodeChange(input.filter { it.isDigit() }.take(OTP_LENGTH)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        decorationBox = { _ ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(OTP_LENGTH) { i ->
                    val char = code.getOrNull(i)?.toString().orEmpty()
                    val active = i == code.length || (i == OTP_LENGTH - 1 && code.length == OTP_LENGTH)
                    val borderColor = when {
                        isError -> Color(0xFFB3261E)
                        active -> BrandGreen
                        char.isNotEmpty() -> BrandGreen.copy(alpha = 0.5f)
                        else -> Border
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.86f)
                            .background(
                                if (char.isNotEmpty()) Color(0xFFF1F6F3) else White,
                                RoundedCornerShape(12.dp)
                            )
                            .border(if (active) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Foreground)
                    }
                }
            }
        }
    )
}

/** "or" separator between two ways of signing in. */
@Composable
fun OrDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Border))
        Text("or", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Border))
    }
}
