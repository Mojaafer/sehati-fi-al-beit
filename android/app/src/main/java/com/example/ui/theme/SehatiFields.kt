package com.example.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/**
 * Shared outlined text-field palette. Several screens (UploadReceipt, the admin search, …) used
 * to force `Color.White` containers, which reads as a floating white patch on the alabaster
 * background; these keep every state on surface tokens instead.
 */
@Composable
fun sehatiTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = OutlineVariant,
    focusedContainerColor = SurfaceContainerLowest,
    unfocusedContainerColor = SurfaceContainerLowest,
    cursorColor = Primary,
    focusedLabelColor = PrimaryContainer,
    unfocusedLabelColor = OnSurfaceVariant
)
