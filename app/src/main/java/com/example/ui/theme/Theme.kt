package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeoBlueLight = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandOnPrimaryLight,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    secondary = BrandSecondaryLight,
    onSecondary = BrandOnSecondaryLight,
    secondaryContainer = BrandSecondaryContainerLight,
    onSecondaryContainer = BrandOnSecondaryContainerLight,
    tertiary = BrandTertiaryLight,
    onTertiary = BrandOnTertiaryLight,
    tertiaryContainer = BrandTertiaryContainerLight,
    onTertiaryContainer = BrandOnTertiaryContainerLight,
    background = BrandBackgroundLight,
    onBackground = BrandOnBackgroundLight,
    surface = BrandSurfaceLight,
    onSurface = BrandOnSurfaceLight,
    surfaceVariant = BrandSurfaceVariantLight,
    onSurfaceVariant = BrandOnSurfaceVariantLight,
    outline = BrandOutlineLight,
    outlineVariant = BrandOutlineVariantLight
)

private val NeoBlueDark = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = BrandSecondaryDark,
    onSecondary = BrandOnSecondaryDark,
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = BrandOnSecondaryContainerDark,
    tertiary = BrandTertiaryDark,
    onTertiary = BrandOnTertiaryDark,
    tertiaryContainer = BrandTertiaryContainerDark,
    onTertiaryContainer = BrandOnTertiaryContainerDark,
    background = BrandBackgroundDark,
    onBackground = BrandOnBackgroundDark,
    surface = BrandSurfaceDark,
    onSurface = BrandOnSurfaceDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF047857),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF115E59),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF7DD3FC),
    background = Color(0xFF061A14),
    surface = Color(0xFF0D281E),
    surfaceVariant = Color(0xFF133E2F),
    outline = Color(0xFF4B7262),
    outlineVariant = Color(0xFF234B3B)
)

private val PurpleLight = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF4338CA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = Color(0xFFD946EF),
    background = Color(0xFFFAF5FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F3FF),
    outline = Color(0xFFA78BFA),
    outlineVariant = Color(0xFFDDD6FE)
)

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF4338CA),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFF0ABFC),
    background = Color(0xFF0F071D),
    surface = Color(0xFF1C1236),
    surfaceVariant = Color(0xFF2E1F52),
    outline = Color(0xFF7C67A6),
    outlineVariant = Color(0xFF4A3B6E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: String = "NEO_BLUE",
    content: @Composable () -> Unit
) {
    val colorScheme = when (colorTheme) {
        "EMERALD" -> if (darkTheme) EmeraldDark else EmeraldLight
        "PURPLE" -> if (darkTheme) PurpleDark else PurpleLight
        else -> if (darkTheme) NeoBlueDark else NeoBlueLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
