package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.SahatakForest
import com.example.ui.theme.SahatakInk
import com.example.ui.theme.SahatakOrange
import com.example.ui.theme.SahatakSage
import com.example.ui.components.SehatiPrimaryButton

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    emailLinkSentTo: String?,
    needsEmailForLink: Boolean,
    onSendEmailLink: (String) -> Unit,
    onCompleteEmailLink: (String) -> Unit,
    onChangeEmail: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onPasswordSignIn: (String, String) -> Unit = { _, _ -> },
    onPasswordSignUp: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var createAccount by remember { mutableStateOf(false) }
    var useEmailLink by remember { mutableStateOf(false) }
    var rememberEmail by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .imePadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(500)) + scaleIn(tween(600), initialScale = 0.88f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_sahatak_logo),
                    contentDescription = "شعار صحتك",
                    modifier = Modifier.size(128.dp)
                )
                Text("صحتك", style = MaterialTheme.typography.headlineLarge,
            color = SahatakInk, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp))
                Text("Sahatak", style = MaterialTheme.typography.titleLarge,
            color = SahatakInk, fontWeight = FontWeight.Medium)
                Text("رعاية صحية منزلية أقرب إليك", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(450, delayMillis = 180)) +
                slideInVertically(tween(500, delayMillis = 180)) { it / 5 }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                if (needsEmailForLink) {
                    EmailLinkCompletion(email, { email = it }, isLoading, errorMessage, onCompleteEmailLink)
                } else if (emailLinkSentTo != null) {
                    EmailLinkSentState(emailLinkSentTo, isLoading, onSendEmailLink, onChangeEmail)
                } else {
                    Row(Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp)) {
                        AuthModeTab("تسجيل الدخول", !createAccount) { createAccount = false }
                        AuthModeTab("حساب جديد", createAccount) { createAccount = true }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        if (useEmailLink) {
                            EmailLinkForm(email, { email = it }, isLoading, errorMessage, onSendEmailLink)
                        } else {
                            PasswordForm(
                                email = email, onEmailChange = { email = it }, password = password,
                                onPasswordChange = { password = it }, showPassword = showPassword,
                                onShowPasswordChange = { showPassword = !showPassword }, rememberEmail = rememberEmail,
                                onRememberEmailChange = { rememberEmail = it }, isLoading = isLoading,
                                errorMessage = errorMessage, createAccount = createAccount,
                                onSubmit = { if (createAccount) onPasswordSignUp(email, password) else onPasswordSignIn(email, password) }
                            )
                        }
                    }
                    TextButton(onClick = { useEmailLink = !useEmailLink }, enabled = !isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (useEmailLink) "استخدام كلمة المرور بدلاً من الرابط" else "الدخول برابط آمن بدون كلمة مرور")
                    }
                }
                }
            }
        }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(450, delayMillis = 280)) +
                slideInVertically(tween(500, delayMillis = 280)) { it / 6 }
        ) {
            OutlinedButton(onClick = onGoogleSignIn, enabled = !isLoading,
                border = androidx.compose.foundation.BorderStroke(1.dp, SahatakOrange.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SahatakForest),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(54.dp), shape = MaterialTheme.shapes.large) {
                Image(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = "شعار Google",
                    modifier = Modifier.size(22.dp)
                )
                Text("المتابعة باستخدام Google", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
        }
        Text("بياناتك محمية ونستخدمها فقط لتقديم الرعاية المناسبة لك.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable private fun RowScope.AuthModeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val isSelected = selected
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "auth-tab-color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(220),
        label = "auth-tab-content-color"
    )
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            },
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        shape = MaterialTheme.shapes.large) {
        Box(Modifier.fillMaxSize().background(containerColor, MaterialTheme.shapes.large), contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun PasswordForm(email: String, onEmailChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit, showPassword: Boolean, onShowPasswordChange: () -> Unit, rememberEmail: Boolean, onRememberEmailChange: (Boolean) -> Unit, isLoading: Boolean, errorMessage: String?, createAccount: Boolean, onSubmit: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
    Text(if (createAccount) "ابدأ حسابك الصحي" else "مرحباً بعودتك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
    Text(if (createAccount) "سجّل الآن للوصول إلى خدمات الرعاية المنزلية." else "أدخل بياناتك للمتابعة بأمان.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SahatakForest,
        unfocusedBorderColor = SahatakSage,
        focusedLabelColor = SahatakForest,
        cursorColor = SahatakOrange,
        focusedLeadingIconColor = SahatakForest,
        unfocusedLeadingIconColor = SahatakSage
    )
    OutlinedTextField(email, onEmailChange, label = { Text("البريد الإلكتروني") }, leadingIcon = { Icon(Icons.Default.Email, null) }, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 18.dp).testTag("input_email_password"))
    OutlinedTextField(password, onPasswordChange, label = { Text("كلمة المرور") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, colors = fieldColors, trailingIcon = { IconButton(onClick = onShowPasswordChange) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (showPassword) "إخفاء كلمة المرور" else "إظهار كلمة المرور", tint = SahatakForest) } }, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("input_password"))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) { Checkbox(rememberEmail, onRememberEmailChange, colors = CheckboxDefaults.colors(checkedColor = SahatakOrange, checkmarkColor = SahatakInk)); Text("تذكر البريد على هذا الجهاز", style = MaterialTheme.typography.bodySmall) }
    AuthErrorCard(errorMessage)
     SehatiPrimaryButton(
         text = if (createAccount) "إنشاء الحساب" else "تسجيل الدخول",
         onClick = onSubmit,
         enabled = email.isNotBlank() && password.length >= 6,
         isLoading = isLoading,
         modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
     )
    }
}

@Composable private fun EmailLinkForm(email: String, onEmailChange: (String) -> Unit, isLoading: Boolean, errorMessage: String?, onSend: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
    Text("دخول آمن بدون كلمة مرور", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
    Text("سنرسل رابطاً خاصاً إلى بريدك الإلكتروني.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
    OutlinedTextField(email, onEmailChange, label = { Text("البريد الإلكتروني") }, leadingIcon = { Icon(Icons.Default.Email, null) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SahatakForest, unfocusedBorderColor = SahatakSage, focusedLabelColor = SahatakForest, cursorColor = SahatakOrange), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 18.dp).testTag("input_email_link"))
    AuthErrorCard(errorMessage)
     SehatiPrimaryButton(
         text = "إرسال رابط الدخول",
         onClick = { onSend(email) },
         enabled = email.isNotBlank(),
         isLoading = isLoading,
         modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
     )
    }
}

@Composable private fun EmailLinkCompletion(email: String, onEmailChange: (String) -> Unit, isLoading: Boolean, errorMessage: String?, onComplete: (String) -> Unit) { EmailLinkForm(email, onEmailChange, isLoading, errorMessage, onComplete) }

@Composable private fun AuthErrorCard(message: String?) { AnimatedVisibility(!message.isNullOrBlank(), enter = fadeIn() + slideInVertically(), exit = fadeOut(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium).padding(12.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.ErrorOutline, "تنبيه", tint = MaterialTheme.colorScheme.onErrorContainer); Text(message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(start = 10.dp)) } } }

@Composable private fun EmailLinkSentState(email: String, isLoading: Boolean, onResend: (String) -> Unit, onChangeEmail: () -> Unit) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)); Text("تحقق من بريدك الإلكتروني", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp)); Text("أرسلنا رابط الدخول إلى $email", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)); TextButton(onClick = { onResend(email) }, enabled = !isLoading) { Text("إعادة إرسال الرابط") }; TextButton(onClick = onChangeEmail, enabled = !isLoading) { Text("استخدام بريد آخر") } } }
