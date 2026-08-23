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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderEntity
import com.example.ui.viewmodel.CareLoopUiState
import com.example.ui.viewmodel.WellbeingChoice

@Composable
fun DailyCareScreen(
    state: CareLoopUiState,
    recentCompletedOrder: OrderEntity?,
    onCheckIn: (WellbeingChoice) -> Unit,
    onCompleteWeeklyReview: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onBookAgain: (OrderEntity) -> Unit,
    onRequestSupport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember(state.lastCheckInAt) { mutableStateOf(state.lastCheckIn) }
    var reflection by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text("متابعتي الصحية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("كيف حالك اليوم؟", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("اطمئن على نفسك في أقل من دقيقة", color = Color.White.copy(alpha = 0.82f))
                }
            }

            Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WellbeingChoice.entries.forEach { choice ->
                    val isSelected = selected == choice
                    OutlinedButton(
                        onClick = {
                            selected = choice
                            onCheckIn(choice)
                        },
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                selected = choice
                                onCheckIn(choice)
                            }
                        ).testTag("care_choice_${choice.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(choice.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        if (isSelected) Text("تم", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            selected?.let { choice ->
                val urgent = choice == WellbeingChoice.URGENT
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (urgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (urgent) "قد تحتاج لتقييم عاجل" else responseFor(choice),
                            fontWeight = FontWeight.Bold,
                            color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        if (urgent) {
                            Text("لا تنتظر زيارة منزلية روتينية. تواصل مع أقرب جهة طوارئ متاحة في منطقتك.", modifier = Modifier.padding(top = 6.dp))
                        }
                        if (choice == WellbeingChoice.NEED_HELP || urgent) {
                            Button(onClick = onRequestSupport, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Text("طلب مساعدة بشرية", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }

            if (state.weeklyReviewDue) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("مراجعتك الأسبوعية", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
                        Text("مقارنة بالأسبوع الماضي، كيف تشعر؟", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("أفضل", "كما أنا", "أسوأ").forEach { answer ->
                                OutlinedButton(onClick = { reflection = answer }, modifier = Modifier.weight(1f)) { Text(answer) }
                            }
                        }
                        Button(
                            onClick = onCompleteWeeklyReview,
                            enabled = reflection != null,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("complete_weekly_review")
                        ) { Text("حفظ المراجعة") }
                    }
                }
            }

            recentCompletedOrder?.let { order ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("هل تحتاج متابعة؟", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text("آخر خدمة: ${order.serviceTitle} مع ${order.providerName}")
                        Button(onClick = { onBookAgain(order) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text("احجز نفس مقدم الخدمة")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("تذكيرات المتابعة", fontWeight = FontWeight.Bold)
                    Text("رسالة خفيفة واحدة يومياً بحد أقصى", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.remindersEnabled, onCheckedChange = onToggleReminders)
            }
        }
    }
}

private fun responseFor(choice: WellbeingChoice): String = when (choice) {
    WellbeingChoice.WELL -> "جميل. خليك لطيف مع نفسك واستمر في روتينك المعتاد."
    WellbeingChoice.TIRED -> "خذ وقتاً للراحة، وإذا استمر التعب يمكنك طلب متابعة مناسبة."
    WellbeingChoice.PAIN -> "راقب الألم، وإذا كان شديداً أو يزداد اطلب تقييماً من مقدم رعاية."
    WellbeingChoice.NEED_HELP -> "نحن هنا لمساعدتك في ترتيب المتابعة أو الخدمة المناسبة."
    WellbeingChoice.URGENT -> ""
}



