package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.ImagePickerCard
import com.example.ui.components.SehatiScreenAppBar
import com.example.ui.components.StepProgressBar

/** Document slots the admin needs to verify a provider, keyed as stored under `providers.docs`. */
const val DOC_ID = "id"
const val DOC_CERTIFICATE = "certificate"
const val DOC_LICENSE = "license"

@Composable
fun ProviderDocsUploadScreen(
    docUris: Map<String, String>,
    isUploading: Boolean,
    errorMessage: String?,
    onDocSelected: (docType: String, uri: String) -> Unit,
    onSubmitDocs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = listOf(DOC_ID, DOC_CERTIFICATE, DOC_LICENSE).all { docUris[it] != null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SehatiScreenAppBar(
            title = "رفع المستندات",
            onBack = onBack,
            backTestTag = "btn_back_provider_docs"
        )

        Column(modifier = Modifier.padding(20.dp)) {
            StepProgressBar(currentStep = 2, totalSteps = 3, stepLabel = "المعلومات المهنية")

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "رفع المستندات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "يرجى رفع صور عالية الجودة للمستندات التالية لغرض التحقق.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            DocSlot(
                label = "الهوية الوطنية / الجواز",
                hint = "صورة واضحة للوجهين.",
                docType = DOC_ID,
                docUris = docUris,
                isUploading = isUploading,
                onDocSelected = onDocSelected
            )

            DocSlot(
                label = "الشهادة الأكاديمية",
                hint = "(بكالوريوس / دبلوم)",
                docType = DOC_CERTIFICATE,
                docUris = docUris,
                isUploading = isUploading,
                onDocSelected = onDocSelected
            )

            DocSlot(
                label = "التسجيل المهني / بطاقة النقابة",
                hint = "يجب أن تكون سارية المفعول.",
                docType = DOC_LICENSE,
                docUris = docUris,
                isUploading = isUploading,
                onDocSelected = onDocSelected
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
                onClick = onSubmitDocs,
                enabled = allSelected && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_submit_provider_docs")
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isUploading) "جاري رفع المستندات..." else "إرسال المستندات للمراجعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DocSlot(
    label: String,
    hint: String,
    docType: String,
    docUris: Map<String, String>,
    isUploading: Boolean,
    onDocSelected: (docType: String, uri: String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        ImagePickerCard(
            imageUri = docUris[docType],
            emptyTitle = "انقر لاختيار الصورة",
            emptyHint = hint,
            filledTitle = "تم اختيار المستند",
            onImageSelected = { uri -> onDocSelected(docType, uri) },
            isUploading = isUploading,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("doc_picker_$docType")
        )
    }
}



