package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.ui.components.SehatiPrimaryButton

@Composable
fun OrderSuccessScreen(
    order: OrderEntity,
    onProceedToPayment: () -> Unit,
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canPay = OrderStatus.canPatientPay(order.status)
    val title = when (order.status) {
        OrderStatus.ORDER_SENT -> "تم إرسال طلبك"
        OrderStatus.ACCEPTED_BY_PROVIDER, OrderStatus.PAYMENT_PENDING -> "قبل مقدم الخدمة طلبك"
        OrderStatus.REJECTED -> "إشعار التحويل يحتاج توضيح"
        OrderStatus.CANCELLED -> OrderStatus.cancelledLabel(order.cancelledBy)
        OrderStatus.COMPLETED -> "تمت الزيارة"
        else -> OrderStatus.label(order.status)
    }
    val guidance = when (order.status) {
        OrderStatus.ORDER_SENT -> "سنرسل لك إشعاراً فور قبول الطلب من مقدم الخدمة ${order.providerName}. لا تدفع قبل القبول."
        OrderStatus.ACCEPTED_BY_PROVIDER, OrderStatus.PAYMENT_PENDING -> "موعدك مقبول. أكمل التحويل الآن لتأكيد الزيارة."
        OrderStatus.REJECTED -> "راجع بيانات التحويل وأعد رفع صورة واضحة يظهر فيها الرقم والمبلغ والتاريخ."
        OrderStatus.CANCELLED -> order.cancelReason.ifBlank { "هذا الطلب ملغي ولا يحتاج أي إجراء إضافي." }
        OrderStatus.COMPLETED -> "اكتملت الزيارة. يمكنك تقييم الخدمة من صفحة طلباتي."
        else -> "تابع أحدث حالة للطلب من صفحة طلباتي."
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Animated Success Circle Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.inversePrimary),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "مرجع الطلب: #${order.orderNumber}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = guidance,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        // Order Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "تفاصيل الطلب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("الخدمة المطلوبة", order.serviceTitle)
                DetailRow("مقدم الخدمة", "${order.providerName} (${order.providerTitle})")
                DetailRow("الموعد المحدد", "${order.visitDate} - ${order.visitTime}")
                DetailRow("عنوان الزيارة", order.areaLocation)
                DetailRow("المبلغ المطلوب تحويله", "${com.example.data.model.OrderFees.effectivePayableAmountSdg(order).toInt()} ج.س", isBold = true)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Order Progress Stepper Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "حالة الطلب الآن",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Step 1: Sent
                TimelineStepItem(
                    stepNum = "1",
                    title = "تم إرسال طلبك",
                    subtitle = "تم إرسال البيانات لمقدم الخدمة",
                    isCompleted = true,
                    isCurrent = false
                )

                // Step 2: Confirmation / Review
                TimelineStepItem(
                    stepNum = "2",
                    title = "قبول مقدم الخدمة",
                    subtitle = if (canPay) "تم القبول ويمكنك إكمال الدفع" else "لا تدفع قبل قبول مقدم الخدمة",
                    isCompleted = order.status != OrderStatus.ORDER_SENT,
                    isCurrent = order.status == OrderStatus.ORDER_SENT ||
                        order.status == OrderStatus.PAYMENT_UNDER_REVIEW
                )

                // Step 3: Scheduled / Confirmed
                TimelineStepItem(
                    stepNum = "3",
                    title = "مجدول للزيارة",
                    subtitle = "سيصل مقدم الخدمة في الموعد المحدد",
                    isCompleted = order.status == OrderStatus.PAYMENT_CONFIRMED ||
                        order.status == OrderStatus.COMPLETED,
                    isCurrent = false,
                    isLast = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        if (canPay) {
            SehatiPrimaryButton(
                text = if (order.status == OrderStatus.REJECTED) "تصحيح بيانات التحويل" else "إكمال الدفع عبر بنكك",
                onClick = onProceedToPayment,
                modifier = Modifier.fillMaxWidth(),
                testTagValue = "btn_proceed_payment"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoToHome,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_back_home")
        ) {
            Text(
                text = "العودة للرئيسية",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun TimelineStepItem(
    stepNum: String,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.secondary
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNum,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}




