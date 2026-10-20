package cn.aurange.schedule

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF272735)
val Navy = Color(0xFF323044)
val Muted = Color(0xFF716D7C)
val Canvas = Color(0xFFF8F7F4)
val Line = Color(0xFFECEAF0)
val Blue = Color(0xFF5C55D6)
val Lavender = Color(0xFFF0EEFC)
val Amber = Color(0xFFA36B3F)
val Sage = Color(0xFF64866F)

fun phaseColor(type: String): Color = when (type) {
    "prep" -> Color(0xFF367067)
    "open" -> Blue
    "regular" -> Color(0xFF4E7159)
    "exam" -> Color(0xFF946137)
    "close" -> Color(0xFF8054A4)
    else -> Color(0xFF6D6A78)
}
fun phaseBackground(type: String): Color = when (type) {
    "prep" -> Color(0xFFEAF4F0)
    "open" -> Lavender
    "regular" -> Color(0xFFEDF3EB)
    "exam" -> Color(0xFFFCF1E3)
    "close" -> Color(0xFFF3EDF8)
    else -> Color(0xFFF1F0F3)
}

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 30.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 15.sp)
)

@Composable fun ScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, onPrimary = Color.White,
        primaryContainer = Lavender, onPrimaryContainer = Blue, secondary = Sage,
        secondaryContainer = Color(0xFFEDF3EB), background = Canvas, surface = Color.White,
        surfaceContainer = Canvas, surfaceContainerHigh = Color.White, surfaceContainerHighest = Lavender,
        onSurface = Ink, onBackground = Ink, outline = Color(0xFFD3D0DC), outlineVariant = Line,
        surfaceVariant = Canvas, onSurfaceVariant = Muted, error = Color(0xFFB6615C)),
        typography = AppTypography, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(30.dp)), content = content)
}
