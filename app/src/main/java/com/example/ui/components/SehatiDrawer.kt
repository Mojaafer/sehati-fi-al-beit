package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.RoleAccess
import com.example.ui.navigation.Routes
import com.example.ui.navigation.TopLevelDestination
import com.example.ui.viewmodel.UserRole

/**
 * Enhanced Navigation Drawer with rich hero header, grouped navigation, role badges, and app branding.
 */
@Composable
fun SehatiDrawerContent(
    role: UserRole,
    displayName: String,
    email: String,
    isGuest: Boolean,
    unreadCount: Int,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onBecomeProvider: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.testTag("nav_drawer")
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Hero Header
            DrawerHeader(
                role = role,
                displayName = displayName,
                email = email,
                isGuest = isGuest
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Destinations Section
            Text(
                text = "القائمة الرئيسية",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            RoleAccess.destinationsFor(role).forEach { destination ->
                val isSelected = destination.isCurrent(currentRoute)
                NavigationDrawerItem(
                    selected = isSelected,
                    onClick = { onNavigate(destination.route) },
                    icon = {
                        Icon(
                            imageVector = destination.icon(),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    badge = {
                        if (destination == TopLevelDestination.INBOX && unreadCount > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(if (unreadCount > 9) "9+" else "$unreadCount")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("drawer_${destination.name.lowercase()}")
                )
            }

            // Optional Daily Care link for Patients
            if (role == UserRole.PATIENT) {
                NavigationDrawerItem(
                    selected = currentRoute == Routes.DAILY_CARE,
                    onClick = { onNavigate(Routes.DAILY_CARE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (currentRoute == Routes.DAILY_CARE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = "المتابعة الصحية اليومية",
                            fontWeight = if (currentRoute == Routes.DAILY_CARE) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Medical Practitioner Promo Banner (Only for Patients)
            if (role == UserRole.PATIENT) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clickable { onBecomeProvider() }
                        .testTag("drawer_become_provider")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "انضم ككادر طبي معتمد",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "سجل كطبيب أو ممرض وقدم خدماتك",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Logout Action
            NavigationDrawerItem(
                selected = false,
                onClick = onLogout,
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                label = {
                    Text(
                        text = "تسجيل الخروج",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("drawer_logout")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Footer branding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "صحتي في البيت • رعاية منزلية موثوقة",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "ود مدني، ولاية الجزيرة • الإصدار 1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    role: UserRole,
    displayName: String,
    email: String,
    isGuest: Boolean
) {
    val name = displayName.ifBlank { if (isGuest) "زائر" else "مستخدم صحتي" }
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("")
        .ifBlank { "ص" }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Initials Avatar with white ring border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = email.ifBlank { "حساب مفعل" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Role Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.20f))
                .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80)) // Bright green online dot
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${RoleAccess.roleLabel(role)} • نشط الآن",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}



