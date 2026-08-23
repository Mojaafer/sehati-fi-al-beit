package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.TopLevelDestination

/**
 * High-aesthetic Bottom Navigation Bar with pill indicators, unread badges, and role awareness.
 */
@Composable
fun BottomNavBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    unreadCount: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                destinations.forEach { destination ->
                    val isSelected = destination.isCurrent(currentRoute)
                    val interactionSource = remember { MutableInteractionSource() }

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = tween(220),
                        label = "scale"
                    )

                    val activeBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = tween(220),
                        label = "bgColor"
                    )

                    val activeTextColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        },
                        animationSpec = tween(220),
                        label = "textColor"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(activeBgColor)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onNavigate(destination.route) }
                            )
                            .padding(vertical = 6.dp)
                            .testTag("nav_${destination.name.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.scale(iconScale)
                        ) {
                            if (destination == TopLevelDestination.INBOX && unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = if (unreadCount > 9) "9+" else "$unreadCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = destination.icon(),
                                        contentDescription = destination.label,
                                        tint = activeTextColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = destination.icon(),
                                    contentDescription = destination.label,
                                    tint = activeTextColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = activeTextColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shared with [SehatiDrawerContent] so a destination reads the same in the bar and the drawer. */
internal fun TopLevelDestination.icon(): ImageVector = when (this) {
    TopLevelDestination.PATIENT_HOME -> Icons.Default.Home
    TopLevelDestination.PATIENT_ORDERS -> Icons.AutoMirrored.Filled.Assignment
    TopLevelDestination.PROVIDER_WORK -> Icons.Default.MedicalServices
    TopLevelDestination.ADMIN_REVIEW -> Icons.Default.AdminPanelSettings
    TopLevelDestination.INBOX -> Icons.Default.Notifications
    TopLevelDestination.ACCOUNT -> Icons.Default.Person
}



