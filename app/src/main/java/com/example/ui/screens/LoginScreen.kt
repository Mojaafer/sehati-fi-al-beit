package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusColors

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
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("صحتي في البيت", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("رعاية صحية منزلية موثوقة في ود مدني", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(40.dp))

        if (needsEmailForLink) {
            Text("أكمل تسجيل الدخول", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("أدخل نفس البريد الذي طلبت له رابط الدخول.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("input_complete_email_link")
            )
            if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Button(
                onClick = { onCompleteEmailLink(email) },
                enabled = !isLoading && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp).testTag("btn_complete_email_link")
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                else Text("تأكيد البريد والدخول", fontWeight = FontWeight.Bold)
            }
        } else if (emailLinkSentTo != null) {
            EmailLinkSentState(
                email = emailLinkSentTo,
                isLoading = isLoading,
                onResend = { onSendEmailLink(emailLinkSentTo) },
                onChangeEmail = { email = ""; onChangeEmail() }
            )
        } else {
            Text("الدخول عبر البريد الإلكتروني", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Text("سنرسل لك رابط دخول آمن. لا تحتاج إلى كلمة مرور.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("input_email_link")
            )
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            Button(
                onClick = { onSendEmailLink(email) },
                enabled = !isLoading && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(52.dp).testTag("btn_send_email_link"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                else Text("إرسال رابط الدخول", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        OutlinedButton(
            onClick = onGoogleSignIn,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("المتابعة باستخدام Google", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmailLinkSentState(email: String, isLoading: Boolean, onResend: () -> Unit, onChangeEmail: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_link_sent")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), CircleShape)
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text("تحقق من بريدك الإلكتروني", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text("أرسلنا رابط تسجيل الدخول إلى:", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Text(email, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(StatusColors.warning.container, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "💡 ملاحظة: إذا لم تجد الرسالة في صندوق الوارد، يرجى مراجعة مجلد (Spam / الرسائل غير المرغوبة) والضغط على الرابط للمتابعة.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = StatusColors.warning.content,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onResend, enabled = !isLoading, modifier = Modifier.padding(top = 10.dp)) { Text("إعادة إرسال الرابط", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onChangeEmail, enabled = !isLoading) { Text("استخدام بريد إلكتروني آخر") }
    }
}



