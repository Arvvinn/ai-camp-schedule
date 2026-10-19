package cn.aurange.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
val weekDay: DateTimeFormatter = DateTimeFormatter.ofPattern("E", Locale.CHINA)

@Composable fun PhaseBadge(phase: Phase?, label: String? = null) {
    Surface(color = phaseBackground(phase?.type.orEmpty()), shape = RoundedCornerShape(6.dp)) {
        Text(label ?: phase?.label ?: "未开始", color = phaseColor(phase?.type.orEmpty()), fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable fun SectionTitle(title: String, subtitle: String = "", trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        trailing?.invoke()
    }
}

@Composable fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable fun TaskRow(task: TaskOccurrence, completed: Boolean, canEdit: Boolean, overdue: Boolean,
                       onClick: () -> Unit, onComplete: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Checkbox(checked = completed, onCheckedChange = if (canEdit) { { onComplete() } } else null,
            modifier = Modifier.size(40.dp), colors = CheckboxDefaults.colors(checkedColor = phaseColor("regular")))
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("第${task.camp.term}期", fontSize = 11.sp, color = Muted)
                if (task.template.time.isNotEmpty()) Text(task.template.time, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (completed) Muted else Color(0xFFB45309))
                if (overdue && !completed) Text("逾期", color = Color(0xFFB91C1C), fontSize = 11.sp)
                if (task.template.follow && !completed) Text("持续跟进", color = phaseColor("prep"), fontSize = 11.sp)
            }
            Text(task.template.title, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium,
                color = if (completed) Muted else Ink,
                textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.padding(top = 4.dp))
            if (task.template.description.isNotEmpty()) Text(task.template.description,
                fontSize = 12.sp, lineHeight = 18.sp, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp))
            if (task.template.links.isNotEmpty()) Text("查看任务与资料  ›", fontSize = 11.sp, color = Blue,
                modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable fun EmptyCard(title: String, description: String) {
    WhiteCard {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
    }
}

fun isOverdue(task: TaskOccurrence, date: LocalDate): Boolean =
    !task.template.follow && task.template.day != "all" && task.date < date
