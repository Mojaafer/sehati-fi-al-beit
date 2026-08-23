package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.OrderStatus

// Serene Health — primary teal
val Primary = Color(0xFF006168)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF0D7C84)
val OnPrimaryContainer = Color(0xFFD9FCFF)
val InversePrimary = Color(0xFF7CD4DD)

// Secondary — muted sage green
val Secondary = Color(0xFF45664A)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFC3E9C5)
val OnSecondaryContainer = Color(0xFF496A4E)

// Tertiary — warm terracotta
val Tertiary = Color(0xFF84481F)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFA16034)
val OnTertiaryContainer = Color(0xFFFFF3ED)
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
