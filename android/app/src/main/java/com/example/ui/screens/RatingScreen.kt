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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderEntity
import com.example.ui.components.SehatiScreenAppBar
import com.example.ui.theme.bodySmallReadable

private val PRAISE_CHIPS = listOf(
    "الالتزام بالوقت",
    "الاحترافية",
    "النظافة والتعقيم",
    "الرعاية والاهتمام"
)

@Composable
fun RatingScreen(
    order: OrderEntity,
    isSubmitting: Boolean,
    onSubmitRating: (stars: Int, chips: List<String>, comment: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stars by remember { mutableIntStateOf(0) }
    var selectedChips by remember { mutableStateOf(setOf<String>()) }
    var comment by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SehatiScreenAppBar(
            title = "تقييم الخدمة",
            onBack = onBack,
            backTestTag = "btn_back_rating"
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "شكراً لثقتكم بنا",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "كيف كانت تجربتك مع ${order.serviceTitle} من ${order.providerName}؟ رأيك يهمنا لتحسين خدماتنا.",
                style = MaterialTheme.typography.bodySmallReadable,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "التقييم العام",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                (1..5).forEach { value ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { stars = value }
                            .testTag("star_$value"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (value <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$value نجوم",
                            tint = if (value <= stars) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ما الذي أعجبك بشكل خاص؟",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                PRAISE_CHIPS.chunked(2).forEach { rowChips ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowChips.forEach { chip ->
                            val isSelected = chip in selectedChips
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedChips = if (isSelected) {
                                        selectedChips - chip
                                    } else {
                                        selectedChips + chip
                                    }
                                },
                                label = {
                                    Text(
                                        text = chip,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("chip_$chip")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ملاحظات إضافية (اختياري)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("شاركنا تفاصيل تجربتك...", style = MaterialTheme.typography.bodySmall) },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("input_rating_comment")
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onSubmitRating(stars, selectedChips.toList(), comment) },
                enabled = stars > 0 && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_submit_rating")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSubmitting) "جاري إرسال التقييم..." else "إرسال التقييم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



