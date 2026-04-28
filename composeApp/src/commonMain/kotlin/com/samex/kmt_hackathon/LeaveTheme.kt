package com.samex.kmt_hackathon

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samex.kmt_hackathon.core.AppColorTheme

data class LeaveStatusColors(
    val signal: Color,
    val onSignal: Color,
    val signalContainer: Color,
    val onSignalContainer: Color,
    val signalBackground: Color,
    val route: Color,
    val routeContainer: Color,
    val onRouteContainer: Color,
    val finalCall: Color,
    val finalCallContainer: Color,
    val onFinalCallContainer: Color,
    val finalCallBackground: Color,
    val missedContainer: Color,
    val onMissedContainer: Color,
    val missedBackground: Color,
)

data class LeaveThemeSpec(
    val theme: AppColorTheme,
    val label: String,
    val description: String,
    val colorScheme: ColorScheme,
    val statusColors: LeaveStatusColors,
)

private val SunriseStatusColors = LeaveStatusColors(
    signal = Color(0xFF7D7100),
    onSignal = Color(0xFFFFFFFF),
    signalContainer = Color(0xFFEFE19A),
    onSignalContainer = Color(0xFF252000),
    signalBackground = Color(0xFFFFF7DF),
    route = Color(0xFF5F6F30),
    routeContainer = Color(0xFFE3E9C0),
    onRouteContainer = Color(0xFF1C2208),
    finalCall = Color(0xFF9F5F00),
    finalCallContainer = Color(0xFFFFDDAE),
    onFinalCallContainer = Color(0xFF321B00),
    finalCallBackground = Color(0xFFFFF0D6),
    missedContainer = Color(0xFFFFD9CD),
    onMissedContainer = Color(0xFF3B0900),
    missedBackground = Color(0xFFFFF1ED),
)

private val LagoonStatusColors = LeaveStatusColors(
    signal = Color(0xFF006C84),
    onSignal = Color(0xFFFFFFFF),
    signalContainer = Color(0xFFB8EAFF),
    onSignalContainer = Color(0xFF001F2A),
    signalBackground = Color(0xFFEAF8FF),
    route = Color(0xFF356B59),
    routeContainer = Color(0xFFBEEBD7),
    onRouteContainer = Color(0xFF002116),
    finalCall = Color(0xFF9A5B00),
    finalCallContainer = Color(0xFFFFDDB2),
    onFinalCallContainer = Color(0xFF311B00),
    finalCallBackground = Color(0xFFFFF2DF),
    missedContainer = Color(0xFFFFD9D4),
    onMissedContainer = Color(0xFF3A0906),
    missedBackground = Color(0xFFFFF1EF),
)

private val GroveStatusColors = LeaveStatusColors(
    signal = Color(0xFF3F6A24),
    onSignal = Color(0xFFFFFFFF),
    signalContainer = Color(0xFFC4EAA4),
    onSignalContainer = Color(0xFF0C2100),
    signalBackground = Color(0xFFF2FAE9),
    route = Color(0xFF596A2D),
    routeContainer = Color(0xFFDDE9B5),
    onRouteContainer = Color(0xFF1A2005),
    finalCall = Color(0xFF8F6100),
    finalCallContainer = Color(0xFFFFDEA6),
    onFinalCallContainer = Color(0xFF2D1B00),
    finalCallBackground = Color(0xFFFFF3DC),
    missedContainer = Color(0xFFFFD9CF),
    onMissedContainer = Color(0xFF3A0900),
    missedBackground = Color(0xFFFFF1EC),
)

private val BerryStatusColors = LeaveStatusColors(
    signal = Color(0xFF8F4A4C),
    onSignal = Color(0xFFFFFFFF),
    signalContainer = Color(0xFFFFDAD9),
    onSignalContainer = Color(0xFF3B080D),
    signalBackground = Color(0xFFFFF3F2),
    route = Color(0xFF775656),
    routeContainer = Color(0xFFFFDAD9),
    onRouteContainer = Color(0xFF2C1515),
    finalCall = Color(0xFF755A2F),
    finalCallContainer = Color(0xFFFFDDAF),
    onFinalCallContainer = Color(0xFF281800),
    finalCallBackground = Color(0xFFFFF1DE),
    missedContainer = Color(0xFFFFDAD5),
    onMissedContainer = Color(0xFF3B0805),
    missedBackground = Color(0xFFFFF1EF),
)

private val SunriseColorScheme = lightColorScheme(
    primary = Color(0xFFD8C357),
    onPrimary = Color(0xFF272100),
    primaryContainer = SunriseStatusColors.signalContainer,
    onPrimaryContainer = SunriseStatusColors.onSignalContainer,
    secondary = SunriseStatusColors.route,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SunriseStatusColors.routeContainer,
    onSecondaryContainer = SunriseStatusColors.onRouteContainer,
    tertiary = Color(0xFF5F8E68),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4ECCF),
    onTertiaryContainer = Color(0xFF143723),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = SunriseStatusColors.missedContainer,
    onErrorContainer = SunriseStatusColors.onMissedContainer,
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

private val LagoonColorScheme = lightColorScheme(
    primary = Color(0xFF006C84),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LagoonStatusColors.signalContainer,
    onPrimaryContainer = LagoonStatusColors.onSignalContainer,
    secondary = Color(0xFF4C626F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F4),
    onSecondaryContainer = Color(0xFF071F2A),
    tertiary = LagoonStatusColors.route,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LagoonStatusColors.routeContainer,
    onTertiaryContainer = LagoonStatusColors.onRouteContainer,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = LagoonStatusColors.missedContainer,
    onErrorContainer = LagoonStatusColors.onMissedContainer,
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF181C1F),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF181C1F),
    surfaceVariant = Color(0xFFDCE4E9),
    onSurfaceVariant = Color(0xFF40484D),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CD),
    inverseSurface = Color(0xFF2D3134),
    inverseOnSurface = Color(0xFFEFF1F4),
)

private val GroveColorScheme = lightColorScheme(
    primary = Color(0xFF4B662C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = GroveStatusColors.signalContainer,
    onPrimaryContainer = GroveStatusColors.onSignalContainer,
    secondary = Color(0xFF586249),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C8),
    onSecondaryContainer = Color(0xFF161E0C),
    tertiary = Color(0xFF386666),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBEB),
    onTertiaryContainer = Color(0xFF002020),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = GroveStatusColors.missedContainer,
    onErrorContainer = GroveStatusColors.onMissedContainer,
    background = Color(0xFFF8FAEF),
    onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFFDFCF3),
    onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE2E4D6),
    onSurfaceVariant = Color(0xFF45483E),
    outline = Color(0xFF75786C),
    outlineVariant = Color(0xFFC6C8BA),
    inverseSurface = Color(0xFF2F312B),
    inverseOnSurface = Color(0xFFF1F1E8),
)

private val BerryColorScheme = lightColorScheme(
    primary = Color(0xFF8F4A4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BerryStatusColors.signalContainer,
    onPrimaryContainer = BerryStatusColors.onSignalContainer,
    secondary = BerryStatusColors.route,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BerryStatusColors.routeContainer,
    onSecondaryContainer = BerryStatusColors.onRouteContainer,
    tertiary = BerryStatusColors.finalCall,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = BerryStatusColors.finalCallContainer,
    onTertiaryContainer = BerryStatusColors.onFinalCallContainer,
    error = Color(0xFF904A40),
    onError = Color(0xFFFFFFFF),
    errorContainer = BerryStatusColors.missedContainer,
    onErrorContainer = BerryStatusColors.onMissedContainer,
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF221919),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF221919),
    surfaceVariant = Color(0xFFF4DDDC),
    onSurfaceVariant = Color(0xFF524343),
    outline = Color(0xFF857372),
    outlineVariant = Color(0xFFD7C1C1),
    inverseSurface = Color(0xFF382E2E),
    inverseOnSurface = Color(0xFFFFEDEC),
)

private val LocalLeaveStatusColors = staticCompositionLocalOf { SunriseStatusColors }

fun leaveThemeSpec(theme: AppColorTheme): LeaveThemeSpec =
    when (theme) {
        AppColorTheme.Sunrise -> LeaveThemeSpec(
            theme = theme,
            label = "Sunrise",
            description = "Warm and bright",
            colorScheme = SunriseColorScheme,
            statusColors = SunriseStatusColors,
        )
        AppColorTheme.Lagoon -> LeaveThemeSpec(
            theme = theme,
            label = "Lagoon",
            description = "Cool and clear",
            colorScheme = LagoonColorScheme,
            statusColors = LagoonStatusColors,
        )
        AppColorTheme.Grove -> LeaveThemeSpec(
            theme = theme,
            label = "Grove",
            description = "Green and grounded",
            colorScheme = GroveColorScheme,
            statusColors = GroveStatusColors,
        )
        AppColorTheme.Berry -> LeaveThemeSpec(
            theme = theme,
            label = "Berry",
            description = "Soft and lively",
            colorScheme = BerryColorScheme,
            statusColors = BerryStatusColors,
        )
    }

fun leaveThemeSpecs(): List<LeaveThemeSpec> = AppColorTheme.entries.map(::leaveThemeSpec)

@Composable
fun leaveStatusColors(): LeaveStatusColors = LocalLeaveStatusColors.current

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
fun LeaveTheme(
    theme: AppColorTheme = AppColorTheme.Sunrise,
    content: @Composable () -> Unit,
) {
    val spec = leaveThemeSpec(theme)
    CompositionLocalProvider(LocalLeaveStatusColors provides spec.statusColors) {
        MaterialTheme(
            colorScheme = spec.colorScheme,
            typography = LeaveTypography,
            shapes = LeaveShapes,
            content = content,
        )
    }
}
