package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Semantic Color Palette (Repair Shop Dark Mode First + Tuned Light Mode)
val NavyPrimary = Color(0xFF2563EB)         // Royal Tech Blue
val NavyPrimaryContainer = Color(0xFF1E3A8A)
val CharcoalBackground = Color(0xFF0B0F17)   // Deep Charcoal Grey
val DarkSlateSurface = Color(0xFF111827)     // Dark Slate Surface
val DarkSlateElevated = Color(0xFF1F2937)    // Elevated Surface Card
val SlateBorderToken = Color(0xFF374151)     // Neutral Outline Border
val SlateTextPrimary = Color(0xFFF9FAFB)     // High-readability pure text
val SlateTextSecondary = Color(0xFF9CA3AF)   // Subdued secondary text

// Status Lifecycle Semantic Tokens (Dark Mode defaults for backward compatibility)
val StatusPendingAmber = Color(0xFFF59E0B)     // Pending / In Progress
val StatusPendingContainer = Color(0xFF451A03)
val StatusRepairedEmerald = Color(0xFF10B981)  // Repaired / Ready to Deliver
val StatusRepairedContainer = Color(0xFF064E3B)
val StatusAlertCrimson = Color(0xFFEF4444)     // Cancelled / Cannot Repair
val StatusAlertContainer = Color(0xFF450A0A)
val StatusDeliveredSlate = Color(0xFF64748B)   // Delivered / Closed
val StatusDeliveredContainer = Color(0xFF1E293B)

@Immutable
data class ShopSemanticColors(
    val isDark: Boolean,
    val topBarContainer: Color,
    val topBarContent: Color,
    val pending: Color,
    val pendingContainer: Color,
    val repaired: Color,
    val repairedContainer: Color,
    val alert: Color,
    val alertContainer: Color,
    val delivered: Color,
    val deliveredContainer: Color
)

private val DarkShopSemanticColors = ShopSemanticColors(
    isDark = true,
    topBarContainer = NavyPrimaryContainer,
    topBarContent = Color.White,
    pending = StatusPendingAmber,
    pendingContainer = StatusPendingContainer,
    repaired = StatusRepairedEmerald,
    repairedContainer = StatusRepairedContainer,
    alert = StatusAlertCrimson,
    alertContainer = StatusAlertContainer,
    delivered = Color(0xFF94A3B8),
    deliveredContainer = StatusDeliveredContainer
)

private val LightShopSemanticColors = ShopSemanticColors(
    isDark = false,
    topBarContainer = NavyPrimaryContainer,
    topBarContent = Color.White,
    pending = Color(0xFFB45309),
    pendingContainer = Color(0xFFFEF3C7),
    repaired = Color(0xFF047857),
    repairedContainer = Color(0xFFD1FAE5),
    alert = Color(0xFFB91C1C),
    alertContainer = Color(0xFFFEE2E2),
    delivered = Color(0xFF475569),
    deliveredContainer = Color(0xFFF1F5F9)
)

val LocalShopColors = staticCompositionLocalOf { DarkShopSemanticColors }

object ShopTheme {
    val colors: ShopSemanticColors
        @Composable
        get() = LocalShopColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = StatusPendingAmber,
    onSecondary = Color.Black,
    secondaryContainer = StatusPendingContainer,
    onSecondaryContainer = Color(0xFFFEF3C7),
    background = CharcoalBackground,
    onBackground = SlateTextPrimary,
    surface = DarkSlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = DarkSlateElevated,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorderToken,
    error = StatusAlertCrimson,
    onError = Color.White,
    errorContainer = StatusAlertContainer,
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimaryContainer,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = NavyPrimaryContainer,
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val shopColors = if (darkTheme) DarkShopSemanticColors else LightShopSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            // TopAppBar is Royal Navy in both themes, so status bar icons stay light
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalShopColors provides shopColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
