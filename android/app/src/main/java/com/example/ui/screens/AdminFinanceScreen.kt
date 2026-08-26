package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import com.example.data.repository.AdminFinanceSummary
import com.example.ui.components.EmptyState
import com.example.ui.components.SkeletonCard

/**
 * The money picture an admin runs the business from: what patients paid, the platform's cut,
 * what is still owed to providers and one tap to record each Bankak transfer that settles it.
 */
@Composable
fun AdminFinanceScreen(
    summary: AdminFinanceSummary,
    payouts: List<PayoutEntity>,
    onMarkPaid: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    repeatRate: Double? = null,
    isLoading: Boolean = false,
    busyPayoutId: String? = null,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    val accrued = payouts.filter { it.status == PayoutStatus.ACCRUED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_finance")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "الحسابات والمستحقات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                FinanceSummaryGrid(summary)

                // The number the whole business leans on: do patients who finished one visit
                // come back? Hidden until there is data, because an invented zero is worse.
                repeatRate?.let { rate ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "نسبة المرضى المتكررين: ${(rate * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "مستحقات بانتظار التحويل (${accrued.size})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            errorMessage != null -> {
                EmptyState(
                    icon = Icons.Default.CloudOff,
                    title = "تعذر تحميل المستحقات",
                    body = errorMessage,
                    action = {
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_retry_payouts")
                        ) {
                            Text("إعادة المحاولة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.testTag("payouts_error_state")
                )
            }

            isLoading -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    repeat(3) { SkeletonCard(height = 110.dp) }
                }
            }

            accrued.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "لا توجد مستحقات معلقة",
                    body = "ستظهر هنا أرباح مقدمي الخدمة فور إكمال كل زيارة، لتحويلها عبر بنكك وتسجيلها."
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(accrued, key = { it.id }) { payout ->
                        AccruedPayoutCard(
                            payout = payout,
                            isBusy = busyPayoutId == payout.id,
                            onMarkPaid = { onMarkPaid(payout.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/** Two-by-two grid of the four figures; the net position sits underneath them. */
@Composable
private fun FinanceSummaryGrid(summary: AdminFinanceSummary) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FinanceBox(
                title = "المحصّل",
                value = "${summary.collectedSdg.toInt()} ج.س",
                modifier = Modifier.weight(1f)
            )
            FinanceBox(
                title = "عمولة المنصة (15%)",
                value = "${summary.commissionSdg.toInt()} ج.س",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            FinanceBox(
                title = "مستحق للمقدمين",
                value = "${summary.owedToProvidersSdg.toInt()} ج.س",
                modifier = Modifier.weight(1f)
            )
            FinanceBox(
                title = "تم صرفه",
                value = "${summary.paidOutSdg.toInt()} ج.س",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FinanceBox(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun AccruedPayoutCard(
    payout: PayoutEntity,
    isBusy: Boolean,
    onMarkPaid: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payout_card_${payout.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = payout.providerName.ifEmpty { "مقدم خدمة" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "طلب #${payout.orderNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${payout.amountSdg.toInt()} ج.س",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onMarkPaid,
                enabled = !isBusy,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_mark_paid_${payout.id}")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        "تم التحويل عبر بنكك ✓",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}



