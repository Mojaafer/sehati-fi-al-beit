package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ui.components.StepProgressBar
import com.example.ui.components.SehatiScreenAppBar

/** Category key to Arabic label, in the order shown on the home screen. */
internal val PROVIDER_CATEGORIES = listOf(
    "LAB_DRAW" to "سحب عينات",
    "NURSING" to "تمريض منزلي",
    "PHYSIO" to "علاج طبيعي",
    "DOCTOR" to "زيارة طبيب"
)

@Composable
fun ProviderRegisterScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (
        name: String,
        title: String,
        category: String,
        experienceYears: Int,
        priceSdg: Double,
        area: String,
        about: String
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PROVIDER_CATEGORIES.first().first) }
    var experience by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }

    val canSubmit = name.isNotBlank() && title.isNotBlank() &&
        price.toDoubleOrNull() != null && area.isNotBlank() && !isSubmitting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SehatiScreenAppBar(
            title = "التسجيل كمقدم خدمة",
            onBack = onBack,
            backTestTag = "btn_back_provider_register"
        )

        Column(modifier = Modifier.padding(20.dp)) {
            StepProgressBar(currentStep = 1, totalSteps = 3, stepLabel = "البيانات الأساسية")

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "أخبرنا عن خدمتك",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "ستتم مراجعة طلبك من قبل الإدارة قبل ظهور ملفك للمرضى.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            RegisterField(
                value = name,
                onValueChange = { name = it },
                label = "الاسم الكامل",
                placeholder = "مثال: محمد عبدالرحمن",
                testTag = "input_provider_name"
            )

            RegisterField(
                value = title,
                onValueChange = { title = it },
                label = "المسمى المهني",
                placeholder = "مثال: فني مختبرات طبية",
                testTag = "input_provider_title"
            )

            Text(
                text = "نوع الخدمة",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                PROVIDER_CATEGORIES.chunked(2).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { (key, label) ->
                            FilterChip(
                                selected = category == key,
                                onClick = { category = key },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("category_chip_$key")
                            )
                        }
                    }
                }
            }

            RegisterField(
                value = experience,
                onValueChange = { experience = it.filter(Char::isDigit) },
                label = "سنوات الخبرة",
                placeholder = "مثال: 6",
                keyboardType = KeyboardType.Number,
                testTag = "input_provider_experience"
            )

            RegisterField(
                value = price,
                onValueChange = { price = it.filter(Char::isDigit) },
                label = "سعر الزيارة (ج.س)",
                placeholder = "مثال: 15000",
                keyboardType = KeyboardType.Number,
                testTag = "input_provider_price"
            )

            RegisterField(
                value = area,
                onValueChange = { area = it },
                label = "منطقة العمل",
                placeholder = "مثال: ود مدني - حي الدرجة",
                testTag = "input_provider_area"
            )

            RegisterField(
                value = about,
                onValueChange = { about = it },
                label = "نبذة عنك (اختياري)",
                placeholder = "اذكر خبراتك والمستشفيات التي عملت بها...",
                minLines = 3,
                testTag = "input_provider_about"
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSubmit(
                        name.trim(),
                        title.trim(),
                        category,
                        experience.toIntOrNull() ?: 0,
                        price.toDoubleOrNull() ?: 0.0,
                        area.trim(),
                        about.trim()
                    )
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_submit_provider_register")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSubmitting) "جاري الحفظ..." else "التالي: رفع المستندات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .testTag(testTag)
        )
    }
}



