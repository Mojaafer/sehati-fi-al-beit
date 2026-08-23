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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProviderEntity
import com.example.ui.components.AnimatedListItem
import com.example.ui.components.EmptyState
import com.example.ui.components.SehatiScreenAppBar
import com.example.ui.components.SkeletonCard
import com.example.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersListScreen(
    category: String,
    providers: List<ProviderEntity>,
    onSelectProvider: (ProviderEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("متاح الآن") }

    val categoryTitle = when (category) {
        "LAB_DRAW" -> "سحب عينات"
        "NURSING" -> "تمريض منزلي"
        "PHYSIO" -> "علاج طبيعي"
        "DOCTOR" -> "زيارة طبيب"
        else -> "جميع مقدمي الخدمة"
    }

    val filteredProviders = remember(category, selectedFilter, providers) {
        var list = if (category == "ALL") providers else providers.filter { it.serviceCategory == category }
        when (selectedFilter) {
            "متاح الآن" -> list.sortedByDescending { it.isAvailableNow }
            "الأقرب" -> list.sortedBy { it.distanceKm }
            "الأعلى تقييماً" -> list.sortedByDescending { it.rating }
            "السعر" -> list.sortedBy { it.priceSdg }
            else -> list
        }
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
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
            SehatiScreenAppBar(
                title = categoryTitle,
                onBack = onBack,
                backTestTag = "btn_back_providers"
            )
            }
        }

        // Section Title
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "اختر مقدم الخدمة المناسب",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "مرتبة حسب التوفر والقرب والتقييم",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            val filters = listOf("متاح الآن", "الأقرب", "الأعلى تقييماً", "السعر")
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_$filter")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Providers List
        when {
            isLoading -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    repeat(3) { SkeletonCard(height = 230.dp) }
                }
            }

            filteredProviders.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = "لا يوجد مقدمو خدمة في هذا القسم بعد",
                    body = "نضيف مقدمي خدمة جدد باستمرار. جرّب قسماً آخر أو عد لاحقاً.",
                    action = {
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_empty_providers_back")
                        ) {
                            Text(text = "تصفح الأقسام", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    itemsIndexed(filteredProviders, key = { _, provider -> provider.id }) { index, provider ->
                        AnimatedListItem(index = index) {
                            ProviderDetailCard(
                                provider = provider,
                                onBookClick = { onSelectProvider(provider) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ProviderDetailCard(
    provider: ProviderEntity,
    onBookClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Top Row: Avatar + Info + Available Tag
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.inversePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (provider.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "موثق",
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = provider.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = "خبرة ${provider.experienceYears} سنوات • ${provider.area}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Availability Tag
                val availability = if (provider.isAvailableNow) StatusColors.success else StatusColors.neutral
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(availability.container)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (provider.isAvailableNow) "متاح الآن" else "متاح لاحقاً",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = availability.content
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Row: Rating, Distance, Price
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = provider.rating.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "(${provider.reviewsCount}) التقييم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${provider.distanceKm} كم",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "المسافة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Price
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${provider.priceSdg.toInt()} ج.س",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "سعر الخدمة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            if (provider.isAvailableNow) {
                Button(
                    onClick = onBookClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_book_${provider.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اطلب الآن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onBookClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_book_later_${provider.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حجز موعد لاحقاً",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}



