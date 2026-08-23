package com.example.ui.screens

import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.OrderEntity
import com.example.ui.components.ImagePickerCard
import com.example.ui.components.SehatiScreenAppBar

@Composable
fun UploadReceiptScreen(
    order: OrderEntity,
    paymentMethod: String,
    senderName: String,
    refNum: String,
    receiptUri: String?,
    isUploading: Boolean,
    onUpdateDetails: (senderName: String, refNum: String) -> Unit,
    onReceiptSelected: (String) -> Unit,
    onSubmitReceipt: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var inputSenderName by remember { mutableStateOf(senderName) }
    var inputRefNum by remember { mutableStateOf(refNum) }

    Column(
        modifier = modifier
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
                    title = "تفاصيل التحويل وإرفاق الإشعار",
                    onBack = onBack,
                    backTestTag = "btn_back_upload"
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Bank Account Card Box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.inversePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "حساب التحويل ($paymentMethod)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "بنك الخرطوم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    BankDetailItem(label = "اسم الحساب", value = BuildConfig.BANK_ACCOUNT_NAME)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Account Number with Copy button. Single source in BuildConfig — the
                    // number used to be pasted three times in this file, guaranteed to drift.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("رقم الحساب للتحويل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(BuildConfig.BANK_ACCOUNT_NUMBER, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    clipboard.setText(AnnotatedString(BuildConfig.BANK_ACCOUNT_NUMBER))
                                    Toast
                                        .makeText(context, "تم نسخ رقم الحساب ${BuildConfig.BANK_ACCOUNT_NUMBER}", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_copy_acc")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // The payable amount carries this order's unique suffix, so its bank-statement
                    // line can be matched to this order by amount alone.
                    BankDetailItem(
                        label = "المبلغ المطلوب تحويله",
                        value = "${com.example.data.model.OrderFees.effectivePayableAmountSdg(order).toInt()} ج.س",
                        isAccent = true
                    )
                    Text(
                        text = "حوّل هذا المبلغ بالضبط — فرق بسيط عن سعر الخدمة يُستخدم لمطابقة تحويلك مع طلبك.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BankDetailItem(label = "مرجع الطلب", value = "#${order.orderNumber}")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inputs for Transfer details
            Text(
                text = "بيانات العملية في إشعار التحويل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = inputSenderName,
                onValueChange = {
                    inputSenderName = it
                    onUpdateDetails(inputSenderName, inputRefNum)
                },
                label = { Text("اسم صاحب الحساب المحول منه", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("input_sender_name")
            )

            OutlinedTextField(
                value = inputRefNum,
                onValueChange = {
                    inputRefNum = it
                    onUpdateDetails(inputSenderName, inputRefNum)
                },
                label = { Text("رقم العملية / المرجع في الإشعار", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("input_ref_num")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Screenshot / Receipt Upload Box
            Text(
                text = "إرفاق صورة إشعار التحويل (Screenshot)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ImagePickerCard(
                imageUri = receiptUri,
                emptyTitle = "انقر لاختيار إشعار التحويل من الاستوديو",
                emptyHint = "يرجى التأكد من وضوح رقم العملية والمبلغ وتاريخ التحويل",
                filledTitle = "تم اختيار صورة الإشعار بنجاح",
                onImageSelected = onReceiptSelected,
                isUploading = isUploading,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .testTag("upload_receipt_card")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            Button(
                onClick = {
                    onUpdateDetails(inputSenderName, inputRefNum)
                    onSubmitReceipt()
                },
                enabled = receiptUri != null && inputSenderName.isNotBlank() &&
                    inputRefNum.isNotBlank() && !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_receipt")
            ) {
                Text(
                    text = if (isUploading) "جاري رفع الإشعار..." else "إرسال الإشعار للمراجعة والتأكيد",
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
private fun BankDetailItem(label: String, value: String, isAccent: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}



