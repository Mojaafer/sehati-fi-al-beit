package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.components.rememberImageModel
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ProviderEntity
import com.example.ui.components.AnimatedListItem
import com.example.ui.components.CancelOrderDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.SkeletonCard
import com.example.ui.theme.StatusColors
import com.example.ui.theme.sehatiTextFieldColors

@Composable
fun AdminDashboardScreen(
    orders: List<OrderEntity>,
    onApprovePayment: (String) -> Unit,
    onRejectPayment: (String) -> Unit,
    modifier: Modifier = Modifier,
    pendingProviders: List<ProviderEntity> = emptyList(),
    onApproveProvider: (String) -> Unit = {},
    onRejectProvider: (String, String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    busyOrderId: String? = null,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onGrantRefund: (String) -> Unit = {},
    onDeclineRefund: (String) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Pending, 1: Confirmed, 2: All
    var searchQuery by remember { mutableStateOf("") }

    val filteredOrders = remember(orders, selectedTabIndex, searchQuery) {
        var list = when (selectedTabIndex) {
            // Refund requests land in the same review queue as receipts: both are money
            // decisions only an admin can settle.
            0 -> orders.filter {
                it.status == OrderStatus.PAYMENT_UNDER_REVIEW ||
                    it.status == OrderStatus.REFUND_REQUESTED
            }
            1 -> orders.filter {
                it.status == OrderStatus.PAYMENT_CONFIRMED || it.status == OrderStatus.COMPLETED
            }
            else -> orders
        }
        if (searchQuery.isNotEmpty()) {
            list = list.filter {
                it.orderNumber.contains(searchQuery, ignoreCase = true) ||
                it.patientName.contains(searchQuery, ignoreCase = true) ||
                it.transferRefNum.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "التحصيل والمدفوعات 🏦",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "مطابقة إشعارات التحويل البنكي مع الحساب وإصدار الاعتمادات",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Summary Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatChip(
                        title = "جديدة للمراجعة",
                        value = "${orders.count { it.status == OrderStatus.PAYMENT_UNDER_REVIEW }} إشعار",
                        bgColor = StatusColors.warning.container,
                        textColor = StatusColors.warning.content,
                        modifier = Modifier.weight(1f)
                    )

                    AdminStatChip(
                        title = "مؤكد اليوم",
                        value = "${orders.filter { it.status == OrderStatus.PAYMENT_CONFIRMED }.sumOf { it.priceSdg }.toInt()} ج.س",
                        bgColor = StatusColors.success.container,
                        textColor = StatusColors.success.content,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The finance ledger is where the business is actually run from, so it gets a
                // permanent door from the collection dashboard rather than a buried menu item.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f))
                        .clickable(onClick = onOpenFinance)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("btn_open_finance")
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الحسابات والمستحقات",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Search Input
        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث برقم الطلب أو المرجع أو اسم المريض...", style = MaterialTheme.typography.bodySmall) },
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = sehatiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_input")
            )
        }

        // Filter Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("قيد المراجعة ⌛", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.testTag("tab_pending")
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("معتمدة ✓", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.testTag("tab_confirmed")
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("الكل", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.testTag("tab_all")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Receipts List
        when {
            // Ahead of the rows on purpose: a refused listen leaves stale cached orders in hand,
            // and approving one of those would look like it worked and change nothing.
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
                        .padding(horizontal = 16.dp)
                ) {
                    repeat(3) { SkeletonCard(height = 210.dp) }
                }
            }

            filteredOrders.isEmpty() && pendingProviders.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = when {
                        searchQuery.isNotEmpty() -> "لا توجد نتائج للبحث"
                        selectedTabIndex == 0 -> "لا توجد إشعارات تحويل للمراجعة"
                        selectedTabIndex == 1 -> "لم يتم اعتماد أي دفعة بعد"
                        else -> "لا توجد طلبات بعد"
                    },
                    body = if (searchQuery.isNotEmpty()) {
                        "جرّب رقم طلب أو مرجع تحويل أو اسم مريض آخر."
                    } else {
                        "ستظهر هنا إشعارات التحويل فور رفعها من المرضى لمطابقتها واعتمادها."
                    }
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (pendingProviders.isNotEmpty()) {
                        item {
                            Text(
                                text = "طلبات انضمام مقدمي الخدمة",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(pendingProviders, key = { it.id }) { provider ->
                            PendingProviderCard(
                                provider = provider,
                                onApprove = { onApproveProvider(provider.id) },
                                onReject = { reason -> onRejectProvider(provider.id, reason) }
                            )
                        }
                    }

                    itemsIndexed(filteredOrders, key = { _, order -> order.id }) { index, order ->
                        AnimatedListItem(index = index) {
                            AdminReceiptCard(
                                order = order,
                                isBusy = busyOrderId == order.id,
                                onApprove = { onApprovePayment(order.id) },
                                onReject = { onRejectPayment(order.id) },
                                onGrantRefund = { onGrantRefund(order.id) },
                                onDeclineRefund = { onDeclineRefund(order.id) }
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

@Composable
private fun AdminStatChip(title: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = textColor)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun PendingProviderCard(
    provider: ProviderEntity,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    var openedDoc by remember { mutableStateOf<String?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_application_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${provider.title} • ${provider.area}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${provider.experienceYears} سنوات خبرة • ${provider.priceSdg.toInt()} ج.س للزيارة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProviderDocumentStrip(
                docs = provider.docs,
                providerId = provider.id,
                onOpenDoc = { openedDoc = it }
            )

            openedDoc?.let { docType ->
                ImageViewerDialog(
                    reference = provider.docs[docType],
                    contentDescription = "${providerDocLabel(docType)} لمقدم الخدمة ${provider.name}",
                    caption = "${provider.name} — ${providerDocLabel(docType)}",
                    onDismiss = { openedDoc = null }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onApprove,
                    // Approving without the paperwork is the one mistake that cannot be walked
                    // back: the account gets the PROVIDER role and starts taking real patients.
                    enabled = provider.docs.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_approve_provider_${provider.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اعتماد ✓", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_reject_provider_${provider.id}")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "رفض",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (provider.docs.isEmpty()) {
                Text(
                    text = "لم يرفع مقدم الخدمة مستنداته بعد، لا يمكن الاعتماد قبل مراجعتها.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (showRejectDialog) {
                CancelOrderDialog(
                    title = "رفض طلب الانتساب",
                    message = "سيصل السبب إلى ${provider.name} في إشعاراته، فاذكر ما ينبغي تصحيحه.",
                    reasons = PROVIDER_REJECTION_REASONS,
                    confirmLabel = "تأكيد الرفض",
                    onConfirm = { reason ->
                        showRejectDialog = false
                        onReject(reason)
                    },
                    onDismiss = { showRejectDialog = false }
                )
            }
        }
    }
}

/** Arabic label for a document slot; the map keys come from [ProviderDocsUploadScreen]. */
private fun providerDocLabel(docType: String): String = when (docType) {
    DOC_ID -> "الهوية الوطنية"
    DOC_CERTIFICATE -> "الشهادة الأكاديمية"
    DOC_LICENSE -> "التسجيل المهني"
    else -> "مستند"
}

/**
 * The documents are the whole basis of the decision, so they are shown as thumbnails rather than
 * counted. Each opens full screen, because a licence expiry date is unreadable at this size.
 */
@Composable
private fun ProviderDocumentStrip(
    docs: Map<String, String>,
    providerId: String,
    onOpenDoc: (String) -> Unit
) {
    if (docs.isEmpty()) {
        Text(
            text = "لا توجد مستندات مرفوعة",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "المستندات (${docs.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "اضغط للتكبير",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        // A fixed slot order keeps the strip stable; the map's own order is not guaranteed.
        listOf(DOC_ID, DOC_CERTIFICATE, DOC_LICENSE).forEach { docType ->
            val reference = docs[docType] ?: return@forEach
            Column(modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = rememberImageModel(reference).value,
                    contentDescription = providerDocLabel(docType),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onOpenDoc(docType) }
                        .testTag("provider_doc_${providerId}_$docType")
                )
                Text(
                    text = providerDocLabel(docType),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Wording an admin picks from when turning an application down. */
private val PROVIDER_REJECTION_REASONS = listOf(
    "المستندات غير واضحة",
    "الشهادة غير مكتملة",
    "التسجيل المهني منتهي",
    "البيانات لا تطابق المستندات"
)

@Composable
private fun AdminReceiptCard(
    order: OrderEntity,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onGrantRefund: () -> Unit = {},
    onDeclineRefund: () -> Unit = {}
) {
    val isCancelled = order.status == OrderStatus.CANCELLED
    val isPending = !isCancelled &&
        order.status == OrderStatus.PAYMENT_UNDER_REVIEW
    val isConfirmed = order.status == OrderStatus.PAYMENT_CONFIRMED ||
        order.status == OrderStatus.COMPLETED
    val isRefundRequest = order.status == OrderStatus.REFUND_REQUESTED
    var showReceipt by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receipt_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val chip = if (isConfirmed) StatusColors.success else StatusColors.warning
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(chip.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Receipt,
                            contentDescription = null,
                            tint = chip.content,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(text = "طلب #${order.orderNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "المريض: ${order.patientName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "${order.priceSdg.toInt()} ج.س",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("طريقة التحويل:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.paymentMethod, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("اسم المحول:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.transferSenderName.ifEmpty { order.patientName }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم العملية / المرجع:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.transferRefNum, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer)
                    }
                    // The unique payable amount is the second half of the match: a statement line
                    // must show both this figure and the reference before the payment is approved.
                    if (order.payableAmountSdg > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المبلغ المطلوب تحويله:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${order.payableAmountSdg.toInt()} ج.س",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }
                }
            }

            order.receiptImageUri?.let { receiptUrl ->
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صورة إشعار التحويل",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "اضغط للتكبير",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                AsyncImage(
                    model = rememberImageModel(receiptUrl).value,
                    contentDescription = "إشعار التحويل للطلب ${order.orderNumber}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showReceipt = true }
                        .testTag("receipt_image_${order.id}")
                )

                if (showReceipt) {
                    ImageViewerDialog(
                        reference = receiptUrl,
                        contentDescription = "إشعار التحويل للطلب ${order.orderNumber}",
                        caption = "طلب #${order.orderNumber} — رقم العملية: ${order.transferRefNum}",
                        onDismiss = { showReceipt = false }
                    )
                }
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onApprove,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_approve_${order.id}")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اعتماد الدفع ✓", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_reject_${order.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفض / توضيح", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else if (isRefundRequest) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = StatusColors.warning.content, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "المريض يطلب استرجاع المبلغ" +
                            if (order.cancelReason.isNotBlank()) " — ${order.cancelReason}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusColors.warning.content,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onGrantRefund,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_grant_refund_${order.id}")
                    ) {
                        Text("قبول الاسترجاع وإلغاء الطلب ✓", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDeclineRefund,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_decline_refund_${order.id}")
                ) {
                    Text("رفض الاسترجاع واستمرار الحجز", style = MaterialTheme.typography.labelMedium)
                }
            } else if (isCancelled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = StatusColors.neutral.content, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = OrderStatus.cancelledLabel(order.cancelledBy) +
                            if (order.cancelReason.isNotBlank()) " — ${order.cancelReason}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusColors.neutral.content,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = StatusColors.success.content, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تم اعتماد الدفع وتحديث الحجز بنجاح", style = MaterialTheme.typography.labelMedium, color = StatusColors.success.content, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



