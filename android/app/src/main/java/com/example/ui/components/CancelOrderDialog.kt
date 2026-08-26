package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Confirms walking away from an order and collects why. The reason travels to the other party's
 * inbox, so offering ready-made ones keeps that message useful even when nobody types anything.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CancelOrderDialog(
    title: String,
    message: String,
    reasons: List<String>,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = {
                                selectedReason = if (selectedReason == reason) "" else reason
                            },
                            label = { Text(reason, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    placeholder = { Text("سبب آخر (اختياري)", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cancel_reason")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(customReason.ifBlank { selectedReason }) },
                modifier = Modifier.testTag("btn_confirm_cancel")
            ) {
                Text(
                    text = confirmLabel,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_dismiss_cancel")) {
                Text("تراجع", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}

/** Wording a patient sees. */
val PATIENT_CANCEL_REASONS = listOf(
    "تغير موعدي",
    "حجزت مقدم خدمة آخر",
    "السعر مرتفع",
    "لم أعد بحاجة للخدمة"
)

/** Wording a provider sees when declining work. */
val PROVIDER_DECLINE_REASONS = listOf(
    "مرتبط بزيارة أخرى",
    "المنطقة بعيدة",
    "الخدمة خارج تخصصي",
    "الوقت غير مناسب"
)

/** Wording a patient picks from when asking for their money back. */
val PATIENT_REFUND_REASONS = listOf(
    "لم أعد بحاجة للخدمة",
    "تأخر مقدم الخدمة",
    "تم الحجز بالخطأ",
    "ظرف طارئ"
)



