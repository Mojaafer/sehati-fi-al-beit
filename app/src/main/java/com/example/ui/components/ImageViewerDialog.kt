package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlin.math.max
import kotlin.math.min

private const val MAX_ZOOM = 6f
private const val DOUBLE_TAP_ZOOM = 3f

/**
 * Shows an uploaded image at full screen: a transfer receipt, or one of a provider's documents. The
 * card thumbnails crop away exactly the details the admin has to read — a transaction number, a
 * licence expiry date — so this view fits the whole image and allows zooming into it.
 */
@Composable
fun ImageViewerDialog(
    reference: String?,
    contentDescription: String,
    caption: String,
    onDismiss: () -> Unit
) {
    val model by rememberImageModel(reference)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    // Panning past the scaled image's edge would drag it off into empty space with no way back.
    fun clamp(candidate: Offset, atScale: Float): Offset {
        val maxX = max(0f, (viewport.width * (atScale - 1f)) / 2f)
        val maxY = max(0f, (viewport.height * (atScale - 1f)) / 2f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("image_viewer")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewport = it }
                    // Tap first so the transform detector sits inner and claims a pinch before
                    // the tap detector can consume the pointers as a press.
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = min(DOUBLE_TAP_ZOOM, MAX_ZOOM)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val next = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            scale = next
                            offset = if (next == 1f) Offset.Zero else clamp(offset + pan, next)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (model == null) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    AsyncImage(
                        model = model,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .testTag("image_viewer_image")
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .systemBarsPadding()
                    .padding(8.dp)
                    .testTag("btn_close_image_viewer")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = Color.White
                )
            }

            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(16.dp)
            )
        }
    }
}



