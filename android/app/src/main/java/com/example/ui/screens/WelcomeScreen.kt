package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.SahatakInk
import com.example.ui.theme.SahatakOrange
import com.example.ui.theme.SahatakSage

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    var reveal by remember { mutableStateOf(false) }
    val motion = rememberInfiniteTransition(label = "welcome-motion")
    val pulse by motion.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "welcome-pulse"
    )
    LaunchedEffect(Unit) { reveal = true }
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = SahatakSage.copy(alpha = 0.12f),
                radius = size.minDimension * 0.34f * pulse,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.2f)
            )
            drawCircle(
                color = SahatakOrange.copy(alpha = 0.10f),
                radius = size.minDimension * 0.2f * (2f - pulse),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.76f)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(28.dp)
        ) {
            AnimatedVisibility(
                visible = reveal,
                enter = fadeIn(tween(500)) + scaleIn(tween(600), initialScale = 0.82f)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_sahatak_logo),
                    contentDescription = "شعار صحتك",
                    modifier = Modifier.size((132f * pulse).dp)
                )
            }
            Text(
                text = "صحتك",
                style = MaterialTheme.typography.headlineLarge,
                color = SahatakInk,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                text = "رعاية صحية منزلية أقرب إليك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.size(28.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = SahatakOrange
            )
            Text(
                text = "جاري تجهيز رعايتك",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
