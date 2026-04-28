package com.samex.kmt_hackathon

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LeaveSignal = Color(0xFF7D7100)
val LeaveOnSignal = Color(0xFFFFFFFF)
val LeaveSignalContainer = Color(0xFFEFE19A)
val LeaveOnSignalContainer = Color(0xFF252000)

val LeaveRoute = Color(0xFF5F6F30)
val LeaveRouteContainer = Color(0xFFE3E9C0)
val LeaveOnRouteContainer = Color(0xFF1C2208)

val LeaveFinalCall = Color(0xFF9F5F00)
val LeaveFinalCallContainer = Color(0xFFFFDDAE)
val LeaveOnFinalCallContainer = Color(0xFF321B00)

val LeaveMissedContainer = Color(0xFFFFD9CD)
val LeaveOnMissedContainer = Color(0xFF3B0900)

private val LeaveColorScheme = lightColorScheme(
    primary = Color(0xFFD8C357),
    onPrimary = Color(0xFF272100),
    primaryContainer = LeaveSignalContainer,
    onPrimaryContainer = LeaveOnSignalContainer,
    secondary = LeaveRoute,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LeaveRouteContainer,
    onSecondaryContainer = LeaveOnRouteContainer,
    tertiary = Color(0xFF5F8E68),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4ECCF),
    onTertiaryContainer = Color(0xFF143723),
    error = Color(0xFFBA1A1A),
    errorContainer = LeaveMissedContainer,
    onErrorContainer = LeaveOnMissedContainer,
    background = Color(0xFFFFFAEF),
    onBackground = Color(0xFF201D12),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF201D12),
    surfaceVariant = Color(0xFFECE6D8),
    onSurfaceVariant = Color(0xFF504A3E),
    outline = Color(0xFF837A68),
    outlineVariant = Color(0xFFD5CDBD),
    inverseSurface = Color(0xFF353024),
    inverseOnSurface = Color(0xFFF8EFD9),
)

private val LeaveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LeaveTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun LeaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeaveColorScheme,
        typography = LeaveTypography,
        shapes = LeaveShapes,
        content = content,
    )
}
