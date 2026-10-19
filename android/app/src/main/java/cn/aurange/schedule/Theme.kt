package cn.aurange.schedule

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1E293B)
val Ink = Color(0xFF0F172A)
val Muted = Color(0xFF64748B)
val Canvas = Color(0xFFF0F4F8)
val Line = Color(0xFFE2E8F0)
val Blue = Color(0xFF2563EB)
val Amber = Color(0xFFF59E0B)

fun phaseColor(type: String): Color = when (type) {
    "prep" -> Color(0xFF0F766E)
    "open" -> Color(0xFF1D4ED8)
    "regular" -> Color(0xFF15803D)
    "exam" -> Color(0xFFB45309)
    "close" -> Color(0xFF6D28D9)
    else -> Color(0xFF475569)
}
fun phaseBackground(type: String): Color = when (type) {
    "prep" -> Color(0xFFCCFBF1)
    "open" -> Color(0xFFDBEAFE)
    "regular" -> Color(0xFFDCFCE7)
    "exam" -> Color(0xFFFEF3C7)
    "close" -> Color(0xFFEDE9FE)
    else -> Color(0xFFF1F5F9)
}

@Composable fun ScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, onPrimary = Color.White,
        secondary = phaseColor("prep"), background = Canvas, surface = Color.White,
        onSurface = Ink, onBackground = Ink, outline = Line, surfaceVariant = Canvas,
        onSurfaceVariant = Muted, error = Color(0xFFB91C1C)), typography = Typography(), content = content)
}
