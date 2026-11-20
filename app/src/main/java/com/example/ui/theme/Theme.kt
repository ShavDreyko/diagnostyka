package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DiagnosticSemanticColors(
    val criticalFg: Color,
    val criticalBg: Color,
    val errorFg: Color,
    val errorBg: Color,
    val warningFg: Color,
    val warningBg: Color,
    val infoFg: Color,
    val infoBg: Color,
    val successFg: Color,
    val successBg: Color,
    val subtleBorder: Color,
    val codeChipBg: Color
)

private val DarkSemanticColors = DiagnosticSemanticColors(
    criticalFg = SeverityCriticalDark,
    criticalBg = SeverityCriticalBgDark,
    errorFg = SeverityErrorDark,
    errorBg = SeverityErrorBgDark,
    warningFg = SeverityWarningDark,
    warningBg = SeverityWarningBgDark,
    infoFg = SeverityInfoDark,
    infoBg = SeverityInfoBgDark,
    successFg = DiagnosticSuccessDark,
    successBg = DiagnosticSuccessBgDark,
    subtleBorder = ConsoleOutlineSubtleDark,
    codeChipBg = ConsoleSurfaceHighDark
)

private val LightSemanticColors = DiagnosticSemanticColors(
    criticalFg = SeverityCriticalLight,
    criticalBg = SeverityCriticalBgLight,
    errorFg = SeverityErrorLight,
    errorBg = SeverityErrorBgLight,
    warningFg = SeverityWarningLight,
    warningBg = SeverityWarningBgLight,
    infoFg = SeverityInfoLight,
    infoBg = SeverityInfoBgLight,
    successFg = DiagnosticSuccessLight,
    successBg = DiagnosticSuccessBgLight,
    subtleBorder = ConsoleOutlineSubtleLight,
    codeChipBg = ConsoleSurfaceVariantLight
)

val LocalDiagnosticColors = staticCompositionLocalOf { DarkSemanticColors }

private val IndustrialDarkColorScheme = darkColorScheme(
    primary = SafetyAmber,
    onPrimary = Color(0xFF1A0D00),
    primaryContainer = SafetyAmberContainerDark,
    onPrimaryContainer = SafetyAmberBright,
    secondary = TelemetryCyan,
    onSecondary = Color(0xFF001F2E),
    secondaryContainer = TelemetryCyanContainerDark,
    onSecondaryContainer = TelemetryCyan,
    tertiary = DiagnosticSuccessDark,
    onTertiary = Color(0xFF002116),
    background = ConsoleBgDark,
    onBackground = ConsoleTextPrimaryDark,
    surface = ConsoleSurfaceDark,
    onSurface = ConsoleTextPrimaryDark,
    surfaceVariant = ConsoleSurfaceVariantDark,
    onSurfaceVariant = ConsoleTextSecondaryDark,
    surfaceContainerLowest = ConsoleBgDark,
    surfaceContainerLow = ConsoleSurfaceDark,
    surfaceContainer = ConsoleSurfaceVariantDark,
    surfaceContainerHigh = ConsoleSurfaceHighDark,
    outline = ConsoleOutlineDark,
    outlineVariant = ConsoleOutlineSubtleDark,
    error = SeverityCriticalDark,
    onError = Color(0xFF3B080D),
    errorContainer = SeverityCriticalBgDark,
    onErrorContainer = SeverityCriticalDark
)

private val IndustrialLightColorScheme = lightColorScheme(
    primary = SafetyAmberDeep,
    onPrimary = Color.White,
    primaryContainer = SafetyAmberContainerLight,
    onPrimaryContainer = Color(0xFF5C2300),
    secondary = TelemetryCyanDeep,
    onSecondary = Color.White,
    secondaryContainer = TelemetryCyanContainerLight,
    onSecondaryContainer = Color(0xFF064E7A),
    tertiary = DiagnosticSuccessLight,
    onTertiary = Color.White,
    background = ConsoleBgLight,
    onBackground = ConsoleTextPrimaryLight,
    surface = ConsoleSurfaceLight,
    onSurface = ConsoleTextPrimaryLight,
    surfaceVariant = ConsoleSurfaceVariantLight,
    onSurfaceVariant = ConsoleTextSecondaryLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = ConsoleSurfaceLight,
    surfaceContainer = ConsoleSurfaceVariantLight,
    surfaceContainerHigh = ConsoleSurfaceHighLight,
    outline = ConsoleOutlineLight,
    outlineVariant = ConsoleOutlineSubtleLight,
    error = SeverityCriticalLight,
    onError = Color.White,
    errorContainer = SeverityCriticalBgLight,
    onErrorContainer = SeverityCriticalLight
)

val AcoposShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AcoposTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IndustrialDarkColorScheme else IndustrialLightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalDiagnosticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AcoposTypography,
            shapes = AcoposShapes,
            content = content
        )
    }
}
