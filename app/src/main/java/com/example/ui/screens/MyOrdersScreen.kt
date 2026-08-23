package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.ui.theme.statusColorsFor
import com.example.ui.theme.StatusColors
import com.example.ui.components.AnimatedListItem
import com.example.ui.components.CancelOrderDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.PATIENT_CANCEL_REASONS
import com.example.ui.components.PATIENT_REFUND_REASONS
import com.example.ui.components.SkeletonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    orders: List<OrderEntity>,
    onSelectOrder: (OrderEntity) -> Unit,
    modifier: Modifier = Modifier,
    onRateOrder: (OrderEntity) -> Unit = {},
    onCancelOrder: (OrderEntity, String) -> Unit = { _, _ -> },
    onRequestRefund: (OrderEntity, String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    busyOrderId: String? = null,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    var orderToCancel by remember { mutableStateOf<OrderEntity?>(null) }
    var orderToRefund by remember { mutableStateOf<OrderEntity?>(null) }

    orderToCancel?.let { pending ->
        CancelOrderDialog(
            title = "إلغاء الطلب #${pending.orderNumber}",
            message = "سيتم إبلاغ مقدم الخدمة بالإلغاء. إن كنت قد دفعت بالفعل فسيراجع فريق الإدارة استرداد المبلغ.",
            reasons = PATIENT_CANCEL_REASONS,
            confirmLabel = "تأكيد الإلغاء",
            onConfirm = { reason ->
                onCancelOrder(pending, reason)
                orderToCancel = null
            },
            onDismiss = { orderToCancel = null }
        )
    }

    orderToRefund?.let { pending ->
        CancelOrderDialog(
            title = "طلب استرجاع للطلب #${pending.orderNumber}",
            message = "سيستلم فريق الإدارة طلبك ويراجع التحويل، وإذا قُبل يعود المبلغ إلى حسابك عبر بنكك خلال 3 أيام عمل.",
            reasons = PATIENT_REFUND_REASONS,
            confirmLabel = "إرسال طلب الاسترجاع",
            onConfirm = { reason ->
                onRequestRefund(pending, reason)
                orderToRefund = null
            },
            onDismiss = { orderToRefund = null }
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
        ) {
        // App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "طلباتي والزيارات 📋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "متابعة حالة الزيارات المنزلية والدفع",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            errorMessage != null -> {
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
                            Text("إعادة المحاولة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.testTag("orders_error_state")
                )
            }

            isLoading -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    repeat(3) { SkeletonCard(height = 190.dp) }
                }
            }

            orders.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Assignment,
                    title = "لا توجد طلبات بعد",
                    body = "احجز زيارة منزلية من الصفحة الرئيسية وستظهر هنا لمتابعة حالتها والدفع."
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    itemsIndexed(orders, key = { _, order -> order.id }) { index, order ->
                        AnimatedListItem(index = index) {
                            PatientOrderItemCard(
                                order = order,
                                isBusy = busyOrderId == order.id,
                                onClick = { onSelectOrder(order) },
                                onRate = { onRateOrder(order) },
                                onCancel = { orderToCancel = order },
                                onRequestRefund = { orderToRefund = order }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PatientOrderItemCard(
    order: OrderEntity,
    isBusy: Boolean,
    onClick: () -> Unit,
    onRate: () -> Unit,
    onCancel: () -> Unit,
    onRequestRefund: () -> Unit = {}
) {
    val statusText = if (order.status == OrderStatus.CANCELLED) {
        OrderStatus.cancelledLabel(order.cancelledBy)
    } else {
        OrderStatus.label(order.status)
    }

    // One shared palette for every journey; the per-screen hex tables this replaces had already
    // drifted (cancelled was red here, neutral elsewhere).
    val statusStyle = statusColorsFor(order.status)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("my_order_item_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.serviceTitle} • #${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusStyle.container)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusStyle.content
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OrderStatusTimeline(
                status = order.status,
                cancelledBy = order.cancelledBy,
                cancelReason = order.cancelReason
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "الموعد: ${order.visitDate} - ${order.visitTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "العنوان: ${order.areaLocation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (order.status == OrderStatus.CANCELLED && order.cancelReason.isNotBlank()) {
                Text(
                    text = "السبب: ${order.cancelReason}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "المبلغ: ${order.priceSdg.toInt()} ج.س", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "تفاصيل الحجز والدفع", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(16.dp))
                }
            }

            if (order.status == OrderStatus.COMPLETED && !order.isRated) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_rate_order_${order.id}")
                ) {
                    Icon(Icons.Default.StarRate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("قيّم الخدمة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (OrderStatus.canPatientCancel(order.status)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_cancel_order_${order.id}")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إلغاء الطلب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Paid but not yet visited: the money is held, so the way out is a refund request
            // that an admin resolves — plain cancellation is closed past this point on purpose.
            if (OrderStatus.canPatientRequestRefund(order.status)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequestRefund,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StatusColors.warning.content
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_refund_order_${order.id}")
                ) {
                    Text("طلب استرجاع المبلغ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}




