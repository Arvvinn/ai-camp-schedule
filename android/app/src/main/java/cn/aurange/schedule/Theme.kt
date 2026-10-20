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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF303D39)
val Navy = Color(0xFF485750)
val Muted = Color(0xFF6D7771)
val Canvas = Color(0xFFF5F4EF)
val Line = Color(0xFFE3E6DF)
val Blue = Color(0xFF536D70)
val Lavender = Color(0xFFE7EEEA)
val Amber = Color(0xFF977354)
val Sage = Color(0xFF607D6B)

private val AppFont = FontFamily(
    Font(R.font.schedule_sans_regular, FontWeight.Normal),
    Font(R.font.schedule_sans_semibold, FontWeight.Medium),
    Font(R.font.schedule_sans_semibold, FontWeight.SemiBold),
    Font(R.font.schedule_sans_semibold, FontWeight.Bold)
)

fun phaseColor(type: String): Color = when (type) {
    "prep" -> Color(0xFF607D73)
    "open" -> Blue
    "regular" -> Color(0xFF6B7B62)
    "exam" -> Amber
    "close" -> Color(0xFF867487)
    else -> Color(0xFF6D6A78)
}
fun phaseBackground(type: String): Color = when (type) {
    "prep" -> Color(0xFFE9EFEB)
    "open" -> Lavender
    "regular" -> Color(0xFFEDF3EB)
    "exam" -> Color(0xFFF1EAE0)
    "close" -> Color(0xFFEFE9ED)
    else -> Color(0xFFF1F0F3)
}

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 15.sp)
).let { t ->
    fun TextStyle.chinese() = copy(fontFamily = AppFont, letterSpacing = 0.sp)
    t.copy(displayLarge = t.displayLarge.chinese(), displayMedium = t.displayMedium.chinese(), displaySmall = t.displaySmall.chinese(),
        headlineLarge = t.headlineLarge.chinese(), headlineMedium = t.headlineMedium.chinese(), headlineSmall = t.headlineSmall.chinese(),
        titleLarge = t.titleLarge.chinese(), titleMedium = t.titleMedium.chinese(), titleSmall = t.titleSmall.chinese(),
        bodyLarge = t.bodyLarge.chinese(), bodyMedium = t.bodyMedium.chinese(), bodySmall = t.bodySmall.chinese(),
        labelLarge = t.labelLarge.chinese(), labelMedium = t.labelMedium.chinese(), labelSmall = t.labelSmall.chinese())
}

@Composable fun ScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, onPrimary = Color.White,
        primaryContainer = Lavender, onPrimaryContainer = Blue, secondary = Sage,
        secondaryContainer = Color(0xFFEDF3EB), onSecondaryContainer = Ink, background = Canvas, surface = Color.White,
        surfaceContainer = Canvas, surfaceContainerHigh = Color.White, surfaceContainerHighest = Lavender,
        onSurface = Ink, onBackground = Ink, outline = Color(0xFFB9C3BB), outlineVariant = Line,
        surfaceVariant = Canvas, onSurfaceVariant = Muted, error = Color(0xFFB6615C)),
        typography = AppTypography, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(30.dp)), content = content)
}
