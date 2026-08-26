package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.OrderStatus

val SahatakForest = Color(0xFF405447)
val SahatakSage = Color(0xFF879686)
val SahatakOrange = Color(0xFFFF8738)
val SahatakInk = Color(0xFF18362A)

// Serene Health — primary teal
val Primary = SahatakForest
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDCE6DB)
val OnPrimaryContainer = SahatakInk
val InversePrimary = Color(0xFFB6C9B5)

// Secondary — muted sage green
val Secondary = Color(0xFF5D705F)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFDDE8DD)
val OnSecondaryContainer = SahatakInk

// Tertiary — warm terracotta
val Tertiary = SahatakOrange
val OnTertiary = SahatakInk
val TertiaryContainer = Color(0xFFFFE0C7)
val OnTertiaryContainer = SahatakInk
val TertiaryFixedDim = Color(0xFFFFB68A)

// Error
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// Surface ladder — warm alabaster
val Background = Color(0xFFFAF9F6)
val OnBackground = Color(0xFF1A1C1A)
val Surface = Color(0xFFFAF9F6)
val SurfaceDim = Color(0xFFDBDAD7)
val SurfaceBright = Color(0xFFFAF9F6)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF4F3F1)
val SurfaceContainer = Color(0xFFEFEEEB)
val SurfaceContainerHigh = Color(0xFFE9E8E5)
val SurfaceContainerHighest = Color(0xFFE3E2E0)
val OnSurface = Color(0xFF1A1C1A)
val OnSurfaceVariant = Color(0xFF3E494A)
val SurfaceVariant = Color(0xFFE3E2E0)
val InverseSurface = Color(0xFF2F312F)
val InverseOnSurface = Color(0xFFF2F1EE)

// Outline
val Outline = Color(0xFF6E797A)
val OutlineVariant = Color(0xFFBDC9CA)

// Fixed tones
val PrimaryFixed = Color(0xFF98F1F9)
val PrimaryFixedDim = Color(0xFF7CD4DD)
val OnPrimaryFixed = Color(0xFF002022)
val OnPrimaryFixedVariant = Color(0xFF004F54)
val SecondaryFixed = Color(0xFFC6ECC8)
val SecondaryFixedDim = Color(0xFFABD0AD)
val OnSecondaryFixed = Color(0xFF01210B)
val OnSecondaryFixedVariant = Color(0xFF2D4E33)
val TertiaryFixed = Color(0xFFFFDBC8)
val OnTertiaryFixed = Color(0xFF321300)
val OnTertiaryFixedVariant = Color(0xFF6F380F)

// Dark scheme surfaces
val DarkSurface = Color(0xFF0F1712)
val DarkSurfaceContainer = Color(0xFF1E2720)
val DarkOnSurface = Color(0xFFE2E3DE)

/**
 * A status color pair: where the chip sits and what its text/icon reads in.
 *
 * Kept outside the M3 [androidx.compose.material3.ColorScheme], which has no custom slots for
 * "this order is cancelled" — a plain object is the simplest fit for a light-only theme.
 */
data class StatusStyle(val container: Color, val content: Color)

/**
 * The single source of status coloring across every journey. Before these existed, the same
 * four pairs were copy-pasted as raw hex through MyOrders, ProvidersList, AdminDashboard,
 * HomeScreen and OrderStatusTimeline — and had already started drifting apart.
 */
object StatusColors {
    /** Money settled / visit done / provider accepted. */
    val success = StatusStyle(container = Color(0xFFDCFCE7), content = Color(0xFF166534))

    /** Waiting on a human: a receipt under review, an order not yet accepted. */
    val warning = StatusStyle(container = Color(0xFFFEF3C7), content = Color(0xFFB45309))

    /** Needs action now: transfer clarification requested, application turned down. */
    val error = StatusStyle(container = Color(0xFFFEE2E2), content = Color(0xFF991B1B))

    /** Terminal but nobody's fault (cancelled) or purely informational. */
    val neutral = StatusStyle(
        container = SurfaceContainer, // matches MaterialTheme.colorScheme.surfaceContainer
        content = Color(0xFF475569)
    )
}

/** Maps an order's lifecycle position ([OrderStatus]) onto the shared status palette. */
fun statusColorsFor(status: String): StatusStyle = when (status) {
    OrderStatus.ACCEPTED_BY_PROVIDER,
    OrderStatus.PAYMENT_CONFIRMED,
    OrderStatus.COMPLETED -> StatusColors.success

    OrderStatus.REJECTED -> StatusColors.error
    OrderStatus.CANCELLED -> StatusColors.neutral

    else -> StatusColors.warning // ORDER_SENT, PAYMENT_PENDING, PAYMENT_UNDER_REVIEW
}

/**
 * Role identity chips (top bar badge, drawer pill). The provider and admin pairs used to be raw
 * emerald/violet hex living in `SehatiTopBar`, which put two palettes into the system that no
 * token owned; these keep the distinction while staying inside the theme.
 */
object RoleColors {
    val patient = StatusStyle(container = PrimaryContainer, content = Primary)
    val provider = StatusColors.success
    val admin = StatusStyle(container = TertiaryContainer, content = OnTertiaryFixedVariant)
}
