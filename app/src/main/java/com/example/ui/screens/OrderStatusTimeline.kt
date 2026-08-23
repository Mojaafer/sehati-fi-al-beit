package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.ui.theme.StatusColors

/**
 * The four stages every booking passes through. Showing the whole path — not just the current
 * label — is what tells a patient whether they are waiting on the provider, on themselves, or on
 * the admin. A cancelled order abandoned the path, so instead of steps it gets a single neutral
 * line saying so (with the reason when there is one) rather than silently rendering nothing.
 */
@Composable
fun OrderStatusTimeline(
    status: String,
    modifier: Modifier = Modifier,
    cancelledBy: String = "",
    cancelReason: String = ""
) {
    if (status == OrderStatus.CANCELLED) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StatusColors.neutral.container)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = StatusColors.neutral.content,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = OrderStatus.cancelledLabel(cancelledBy) +
                    if (cancelReason.isNotBlank()) " — $cancelReason" else "",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = StatusColors.neutral.content
            )
        }
        return
    }

    val steps = listOf("الطلب", "القبول", "الدفع", "الزيارة")
    val reached = when (status) {
        OrderStatus.ORDER_SENT -> 0
        OrderStatus.ACCEPTED_BY_PROVIDER -> 1
        OrderStatus.PAYMENT_PENDING, OrderStatus.PAYMENT_UNDER_REVIEW, OrderStatus.REJECTED -> 1
        OrderStatus.PAYMENT_CONFIRMED -> 2
        OrderStatus.COMPLETED -> 3
        else -> 0
    }

    val statusHint = when (status) {
        OrderStatus.ORDER_SENT -> "بانتظار قبول الكادر الطبي للموعد المختار"
        OrderStatus.ACCEPTED_BY_PROVIDER, OrderStatus.PAYMENT_PENDING -> "تم قبول طلبك! يرجى إتمام التحويل البنكي ورفع الإشعار"
        OrderStatus.PAYMENT_UNDER_REVIEW -> "إشعار التحويل قيد تدقيق الإدارة وتأكيد الزيارة"
        OrderStatus.PAYMENT_CONFIRMED -> "تم تأكيد الحجز! الكادر الطبي يجهز للزيارة في الموعد"
        OrderStatus.COMPLETED -> "اكتملت الزيارة بنجاح — نتمنى لك دوام الصحة والعافية"
        OrderStatus.REJECTED -> "تم رفض إشعار التحويل، يرجى إعادة إرسال إشعار صحيح"
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            steps.forEachIndexed { index, label ->
                val done = index <= reached
                val dotColor by animateColorAsState(
                    targetValue = if (done) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    label = "timeline_dot"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(dotColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = if (index == reached) FontWeight.Bold else FontWeight.Normal,
                        color = if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (index != steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 9.dp)
                            .height(2.dp)
                            .background(
                                if (index < reached) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                    )
                }
            }
        }

        if (statusHint != null) {
            Text(
                text = statusHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}





