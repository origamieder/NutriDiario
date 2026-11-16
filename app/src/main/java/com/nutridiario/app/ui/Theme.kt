package com.nutridiario.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta de la app
val Green = Color(0xFF16A34A)
val GreenLight = Color(0xFF4ADE80)
val Teal = Color(0xFF0D9488)
val Orange = Color(0xFFF97316)
val ProteinC = Color(0xFF8B5CF6)
val CarbsC = Color(0xFFF59E0B)
val FatC = Color(0xFFEC4899)
val FiberC = Color(0xFF10B981)
val SugarC = Color(0xFFF43F5E)
val SaltC = Color(0xFF0EA5E9)
val Over = Color(0xFFEF4444)
val Good = Color(0xFF22C55E)
val Warn = Color(0xFFF59E0B)

val HeroGradient = Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF0D9488)))

private val Light = lightColorScheme(
    primary = Green, onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7), onPrimaryContainer = Color(0xFF052E16),
    secondary = Orange, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5), onSecondaryContainer = Color(0xFF431407),
    tertiary = ProteinC, tertiaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFFF4F7F5), onBackground = Color(0xFF111827),
    surface = Color(0xFFF4F7F5), onSurface = Color(0xFF111827),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFF1F5F3), surfaceContainerHighest = Color(0xFFE7EDEA),
    surfaceVariant = Color(0xFFE7EDEA), onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF9CA3AF), outlineVariant = Color(0xFFE5E7EB),
)

private val Dark = darkColorScheme(
    primary = GreenLight, onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D), onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFFFB923C), secondaryContainer = Color(0xFF7C2D12),
    tertiary = Color(0xFFA78BFA), tertiaryContainer = Color(0xFF3B2A70),
    background = Color(0xFF0B1210), onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF0B1210), onSurface = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFF121A17), surfaceContainerLow = Color(0xFF151F1B),
    surfaceContainer = Color(0xFF17221E), surfaceContainerHigh = Color(0xFF1D2925), surfaceContainerHighest = Color(0xFF24312C),
    surfaceVariant = Color(0xFF24312C), onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF6B7280), outlineVariant = Color(0xFF2B3833),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val AppType = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)

@Composable
fun NutriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, shapes = AppShapes, typography = AppType, content = content)
}
