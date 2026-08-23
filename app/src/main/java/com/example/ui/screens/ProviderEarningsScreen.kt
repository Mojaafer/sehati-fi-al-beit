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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import com.example.data.repository.ProviderEarnings
import com.example.ui.components.EmptyState
import com.example.ui.components.SkeletonCard

/**
 * The provider's own ledger: what has been earned per visit, what is still owed and what has
 * already been sent to their Bankak account. Every figure comes from the payout feed — nothing
 * is estimated on screen.
 */
@Composable
fun ProviderEarningsScreen(
    earnings: ProviderEarnings,
    payouts: List<PayoutEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_earnings")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "أرباحي ومستحقاتي",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "مستحق لك الآن",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "${earnings.accruedSdg.toInt()} ج.س",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EarningsChip(
                        label = "زيارات بانتظار الصرف",
                        value = "${earnings.accruedCount}",
                        modifier = Modifier.weight(1f)
                    )
                    EarningsChip(
                        label = "تم صرفه سابقاً",
                        value = "${earnings.paidSdg.toInt()} ج.س",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            errorMessage != null -> {
                EmptyState(
                    icon = Icons.Default.CloudOff,
                    title = "تعذر تحميل السجل",
                    body = errorMessage,
                    action = {
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_retry_earnings")
                        ) {
                            Text("إعادة المحاولة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.testTag("earnings_error_state")
                )
            }

            isLoading -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    repeat(3) { SkeletonCard(height = 90.dp) }
                }
            }

            payouts.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Payments,
                    title = "لا توجد أرباح بعد",
                    body = "بعد إكمال كل زيارة مدفوعة يُسجَّل دخلك هنا تلقائياً، ويصرف عبر بنكك."
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(payouts, key = { it.id }) { payout ->
                        EarningsRow(payout)
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EarningsChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EarningsRow(payout: PayoutEntity) {
    val isPaid = payout.status == PayoutStatus.PAID

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("earning_row_${payout.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPaid) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaid) Icons.Default.Payments else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isPaid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "طلب #${payout.orderNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPaid) "تم التحويل إلى حسابك" else "بانتظار التحويل من الإدارة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${payout.amountSdg.toInt()} ج.س",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



