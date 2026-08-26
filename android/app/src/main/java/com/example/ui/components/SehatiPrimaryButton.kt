package com.example.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Shapes

/**
 * The one agreed CTA: semantic primary container, semantic on-primary content, 12dp radius ([Shapes.medium]), 52dp
 * tall. OrderSuccess and PaymentMethod used to hand-roll near-identical buttons off
 * `primaryContainer`, so a second accent color was creeping into the most important tap on
 * each screen. Pass [testTag] via the modifier as usual.
 */
@Composable
fun SehatiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTagValue: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = Shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .height(52.dp)
            .then(if (testTagValue != null) Modifier.testTag(testTagValue) else Modifier)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}



