package cn.aurange.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
val weekDay: DateTimeFormatter = DateTimeFormatter.ofPattern("E", Locale.CHINA)

@Composable fun AppMark(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Blue), contentAlignment = Alignment.Center) {
        AppIcon(Glyph.CalendarCheck, modifier = Modifier.size(21.dp), tint = Color.White)
    }
}

@Composable fun PhaseBadge(phase: Phase?, label: String? = null) {
    Surface(color = phaseBackground(phase?.type.orEmpty()), shape = RoundedCornerShape(8.dp)) {
        Text(label ?: phase?.label ?: "未开始", color = phaseColor(phase?.type.orEmpty()), fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable fun SectionTitle(title: String, subtitle: String = "", trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 3.dp))
        }
        trailing?.invoke()
    }
}

@Composable fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line.copy(alpha = .7f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable fun CompletionRing(completed: Int, total: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val target = if (total == 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, tween(300), label = "completion")
    val foreground = if (onDark) Color.White else Blue
    Box(modifier.size(78.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(if (onDark) Color.White.copy(alpha = .19f) else Lavender, -90f, 360f, false, style = stroke)
            if (progress > 0f) drawArc(foreground, -90f, progress * 360f, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(target * 100).toInt()}%", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = foreground)
            Text("完成", fontSize = 9.sp, color = foreground.copy(alpha = .7f))
        }
    }
}

@Composable fun TaskCard(task: TaskOccurrence, completed: Boolean, canEdit: Boolean, overdue: Boolean,
                        onClick: () -> Unit, onComplete: () -> Unit) {
    val background by animateColorAsState(if (completed) Color(0xFFFBFAFE) else Color.White, tween(180), label = "task-color")
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), color = background,
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (overdue && !completed) Color(0xFFF0DDD2) else Line.copy(alpha = .65f))) {
        Row(Modifier.padding(start = 8.dp, end = 15.dp, top = 12.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            TaskCompletionControl(completed, canEdit, onComplete)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(5.dp).background(phaseColor(task.phase.type), CircleShape))
                    Text("第${task.camp.term}期 · ${task.phase.label}", fontSize = 10.sp, color = Muted, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (task.template.time.isNotEmpty()) Text(task.template.time, fontSize = 11.sp, color = if (overdue) Amber else Blue,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Text(task.template.title, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                    color = if (completed) Navy else Ink,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                if (task.template.description.isNotEmpty()) Text(task.template.description, fontSize = 11.sp, lineHeight = 18.sp,
                    color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            if (task.template.links.isNotEmpty()) AppIcon(Glyph.ArrowUpRight, modifier = Modifier.padding(start = 8.dp).size(15.dp), tint = Muted.copy(alpha = .6f))
        }
    }
}

@Composable fun EmptyCard(title: String, description: String) {
    WhiteCard {
        Box(Modifier.size(44.dp).background(Lavender, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { AppIcon(Glyph.Coffee, tint = Blue) }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun ActionRow(icon: Glyph, title: String, description: String = "", status: String = "", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(35.dp).background(Lavender, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { AppIcon(icon, tint = Blue, modifier = Modifier.size(18.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
            if (description.isNotEmpty()) Text(description, fontSize = 10.sp, lineHeight = 17.sp, color = Muted,
                modifier = Modifier.padding(top = 4.dp))
        }
        if (status.isNotEmpty()) Text(status, fontSize = 11.sp, color = Blue)
        AppIcon(Glyph.ChevronRight, modifier = Modifier.size(14.dp), tint = Muted.copy(alpha = .6f))
    }
}
