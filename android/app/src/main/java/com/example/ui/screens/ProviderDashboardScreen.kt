package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.ui.components.AnimatedListItem
import com.example.ui.components.CancelOrderDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.PROVIDER_DECLINE_REASONS
import com.example.ui.components.SkeletonCard
import com.example.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    isAvailable: Boolean,
    onToggleAvailability: () -> Unit,
    orders: List<OrderEntity>,
    onAcceptOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCompleteOrder: (String) -> Unit = {},
    onDeclineOrder: (String, String) -> Unit = { _, _ -> },
    onCallPatient: (String) -> Unit = {},
    providerName: String = "",
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    isSavingAvailability: Boolean = false,
    busyOrderId: String? = null,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onOpenEarnings: () -> Unit = {}
) {
    val name = providerName.ifBlank { "مقدم الخدمة" }
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")

    // Every figure below is derived from the live order feed. A dashboard that reported invented
    // numbers would be worse than one that reported none. The provider earns their 85% share of
    // each visit — the platform's cut never appears as if it were theirs.
    val newRequests = orders.filter { it.status == OrderStatus.ORDER_SENT }
    val scheduled = orders.filter { it.status == OrderStatus.PAYMENT_CONFIRMED }
    val completed = orders.filter { it.status == OrderStatus.COMPLETED }
    val earned = completed.sumOf { com.example.data.model.OrderFees.effectiveProviderPayoutSdg(it) }

    var orderToDecline by remember { mutableStateOf<OrderEntity?>(null) }

    orderToDecline?.let { pending ->
        CancelOrderDialog(
            title = "الاعتذار عن الطلب #${pending.orderNumber}",
            message = "سيُبلَّغ المريض بالاعتذار ويمكنه اختيار مقدم خدمة آخر.",
            reasons = PROVIDER_DECLINE_REASONS,
            confirmLabel = "تأكيد الاعتذار",
            onConfirm = { reason ->
                onDeclineOrder(pending.id, reason)
                orderToDecline = null
            },
            onDismiss = { orderToDecline = null }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.inversePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inversePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (isAvailable) "متصل الآن" else "غير متصل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AvailabilityCard(
                    isAvailable = isAvailable,
                    isSaving = isSavingAvailability,
                    onToggle = onToggleAvailability
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricBox(
                title = "طلبات جديدة",
                value = "${newRequests.size}",
                icon = Icons.Default.AssignmentTurnedIn,
                bgColor = MaterialTheme.colorScheme.secondaryContainer,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            MetricBox(
                title = "دخل مكتمل",
                value = "${earned.toInt()} ج.س",
                icon = Icons.Default.Payments,
                bgColor = StatusColors.success.container,
                iconColor = StatusColors.success.content,
                modifier = Modifier.weight(1f)
            )

            MetricBox(
                title = "زيارات تمت",
                value = "${completed.size}",
                icon = Icons.Default.Star,
                bgColor = StatusColors.warning.container,
                iconColor = StatusColors.warning.content,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenEarnings,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("btn_open_earnings")
        ) {
            Icon(
                Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "أرباحي ومستحقاتي",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "طلبات جديدة بانتظار ردك",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                errorMessage != null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.height(230.dp)) {
                            EmptyState(
                                icon = Icons.Default.CloudOff,
                                title = "تعذر تحميل الطلبات",
                                body = errorMessage,
                                action = {
                                    Button(
                                        onClick = onRetry,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("btn_retry_orders")
                                    ) {
                                        Text(
                                            "إعادة المحاولة",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("orders_error_state")
                            )
                        }
                    }
                }

                isLoading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) { SkeletonCard(height = 200.dp) }
                    }
                }

                newRequests.isEmpty() -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.height(190.dp)) {
                            EmptyState(
                                icon = Icons.Default.Inbox,
                                title = if (isAvailable) "لا توجد طلبات جديدة" else "أنت غير متاح حالياً",
                                body = if (isAvailable) {
                                    "ستظهر هنا فور إرسال المرضى القريبين منك لطلباتهم."
                                } else {
                                    "فعّل حالة العمل بالأعلى لتصلك طلبات جديدة."
                                }
                            )
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        newRequests.forEachIndexed { index, order ->
                            AnimatedListItem(index = index) {
                                IncomingRequestCard(
                                    order = order,
                                    isBusy = busyOrderId == order.id,
                                    onAccept = { onAcceptOrder(order.id) },
                                    onDecline = { orderToDecline = order }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "زياراتك المجدولة (${scheduled.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (scheduled.isEmpty()) {
                Text(
                    text = "لا توجد زيارات مؤكدة بعد. تظهر الزيارة هنا بعد اعتماد الدفع.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scheduled.forEach { order ->
                        ScheduledVisitCard(
                            order = order,
                            isBusy = busyOrderId == order.id,
                            onComplete = { onCompleteOrder(order.id) },
                            onCall = { onCallPatient(order.patientPhone) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * The switch writes through to the provider's catalogue document, so flipping it here is what
 * removes the provider from the patients' "متاح الآن" list rather than a local-only preference.
 */
@Composable
private fun AvailabilityCard(isAvailable: Boolean, isSaving: Boolean, onToggle: () -> Unit) {
    val containerColor by animateColorAsState(
        // Brand teal when open, slate when closed — the card used to glow an off-brand blue
        // that belonged to no token in the system.
        targetValue = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface,
        label = "availability_bg"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAvailable) "حالة العمل: متاح ونستقبل الطلبات" else "حالة العمل: غير متاح حالياً",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = when {
                        isSaving -> "جارٍ حفظ حالتك..."
                        isAvailable -> "تظهر لمرضى منطقتك ضمن المتاحين الآن"
                        else -> "لن تظهر في نتائج البحث ولن تصلك طلبات"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            if (isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("progress_provider_availability")
                )
            } else {
                Switch(
                    checked = isAvailable,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary,
                        checkedTrackColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("switch_provider_availability")
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    order: OrderEntity,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_request_${order.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.serviceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusColors.warning.container)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.visitDate,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.warning.content
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(Icons.Default.Person, "المريض: ${order.patientName}")
            InfoRow(Icons.Default.LocationOn, "العنوان: ${order.areaLocation}")
            InfoRow(Icons.Default.Schedule, "الوقت: ${order.visitTime}")

            if (order.notes.isNotEmpty()) {
                Text(
                    text = "ملاحظات: ${order.notes}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusColors.warning.content,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Text(
                text = "المبلغ: ${order.priceSdg.toInt()} ج.س",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_provider_accept_${order.id}")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("قبول الطلب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_provider_decline_${order.id}")
                ) {
                    Text("اعتذار", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ScheduledVisitCard(
    order: OrderEntity,
    isBusy: Boolean,
    onComplete: () -> Unit,
    onCall: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_scheduled_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.visitTime} • ${order.serviceTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${order.patientName} — ${order.areaLocation}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (order.status == OrderStatus.PAYMENT_CONFIRMED) StatusColors.success.container else StatusColors.warning.container)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (order.status == OrderStatus.PAYMENT_CONFIRMED) "مدفوع ✓" else "بانتظار الدفع",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (order.status == OrderStatus.PAYMENT_CONFIRMED) {
                            StatusColors.success.content
                        } else {
                            StatusColors.warning.content
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // A home visit falls apart over a wrong gate number, so the phone is one tap away —
            // but only on an accepted visit, never on a request the provider has not taken yet.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (order.patientPhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_call_patient_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "اتصال بالمريض",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اتصال", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = onComplete,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_provider_complete_${order.id}")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("إكمال الزيارة ✓", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}



