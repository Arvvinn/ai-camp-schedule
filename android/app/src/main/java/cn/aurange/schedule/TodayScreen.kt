package cn.aurange.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TodayScreen(state: AppState, engine: ScheduleEngine, onTask: (TaskOccurrence) -> Unit,
                            onComplete: (TaskOccurrence) -> Unit) {
    var selectedDate by rememberSaveable(state.today.toString()) { mutableStateOf(state.today.toString()) }
    var showCalendar by remember { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.ALL.name) }
    val filter = TaskFilter.valueOf(filterName)
    val date = LocalDate.parse(selectedDate)
    val snapshot = state.displaySnapshot
    val tasks = remember(snapshot, date) { engine.today(snapshot, date) }
    // Only grouping/removal waits for the animation; display status responds on tap.
    val presentation = rememberCompletionPresentation(snapshot)
    val shownTasks = remember(presentation, date) { engine.today(presentation, date) }
    val completedIds = remember(snapshot) { snapshot.tasks.filterValues { it.completed }.keys }
    val groupedCompletedIds = remember(presentation) { presentation.tasks.filterValues { it.completed }.keys }
    val completed = tasks.count { it.id in completedIds }
    val groups = remember(shownTasks, date, groupedCompletedIds, filter) { taskGroups(shownTasks, date, groupedCompletedIds, filter) }
    val canEdit = state.online && !state.demo && !state.busy

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 60.dp, bottom = floatingContentBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (date == state.today) "今天的安排" else date.format(shortDate), style = MaterialTheme.typography.headlineMedium)
                        if (date == state.today) Text("Today", fontFamily = EditorialFont,
                            fontSize = 19.sp, color = Blue, maxLines = 1)
                    }
                    Text(date.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)), color = Muted,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                }
                if (date != state.today) TextButton(onClick = { selectedDate = state.today.toString() }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("今天", fontSize = 12.sp) }
                OutlinedIconButton(onClick = { showCalendar = true }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line)) {
                    AppIcon(Glyph.Calendar, "选择日期", tint = Ink)
                }
            }
        }
        item {
            TodaySummary(completed, tasks.size, tasks.count { isOverdue(it, date) && it.id !in completedIds },
                tasks.count { it.template.follow && it.id !in completedIds })
        }
        item {
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
                Box(Modifier.padding(horizontal = 7.dp, vertical = 9.dp)) { WeekDateStrip(date, state.today) { selectedDate = it.toString() } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("我的日程", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE7EBF1)) {
                    Row(Modifier.padding(3.dp)) {
                        TaskFilter.entries.forEach { option ->
                            Box(Modifier.clip(RoundedCornerShape(9.dp)).background(if (filter == option) Color.White else Color.Transparent)
                                .selectable(filter == option, role = Role.Tab, onClick = { filterName = option.name })
                                .padding(horizontal = 11.dp, vertical = 8.dp)) {
                                Text(option.label, color = if (filter == option) Blue else Muted, fontSize = 12.sp,
                                    fontWeight = if (filter == option) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
        if (state.snapshot.camps.isEmpty()) item {
            EmptyCard("还没有营期", "同步团队排期后，就能在这里查看每天的安排。")
        } else if (groups.isEmpty()) item {
            EmptyCard(when (filter) { TaskFilter.COMPLETED -> "还没有已完成的事项"; TaskFilter.PENDING -> "待办已全部完成"; else -> "这一天暂无待办" },
                if (filter == TaskFilter.COMPLETED) "完成当天待办后，会在这里显示。" else "到「营期」查看各周安排与相关资料。")
        }
        for (group in groups) {
            item(key = "group-${group.bucket.name}") {
                val accent = when (group.bucket) { TaskBucket.OVERDUE -> Amber; TaskBucket.FOLLOW -> phaseColor("prep"); TaskBucket.COMPLETED -> Sage; else -> Muted }
                Row(Modifier.padding(top = 5.dp, start = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.width(3.dp).height(12.dp).background(accent, RoundedCornerShape(2.dp)))
                    Text(group.bucket.label, fontSize = 12.sp, color = accent, fontWeight = FontWeight.SemiBold)
                    Text("${group.tasks.size}", fontSize = 12.sp, color = Muted)
                }
            }
            items(group.tasks, key = { it.id }) { task ->
                Box(Modifier.animateItem(fadeInSpec = tween(160), fadeOutSpec = tween(160),
                    placementSpec = spring(dampingRatio = .85f, stiffness = 500f))) {
                    TaskCard(task, task.id in completedIds, canEdit && task.id !in state.pendingCompletions,
                        isOverdue(task, date), { onTask(task) }, { onComplete(task) }, saving = task.id in state.pendingCompletions)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                AppIcon(Glyph.Clock, modifier = Modifier.size(12.dp), tint = Muted.copy(alpha = .7f))
                Text("  北京时间 · 点击任务查看详情", fontSize = 12.sp, color = Muted.copy(alpha = .8f))
            }
        }
    }
    if (showCalendar) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 2000..2100)
        DatePickerDialog(onDismissRequest = { showCalendar = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let {
                selectedDate = java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
            }; showCalendar = false }) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { selectedDate = state.today.toString(); showCalendar = false }) { Text("回到今天") } }) {
            DatePicker(picker, title = { Text("选择日程日期", modifier = Modifier.padding(24.dp)) }, showModeToggle = false)
        }
    }
}

@Composable
private fun rememberCompletionPresentation(snapshot: Snapshot): Snapshot {
    var displayed by remember { mutableStateOf(snapshot) }
    LaunchedEffect(snapshot) {
        if (displayed.camps == snapshot.camps &&
            displayed.tasks.filterValues { it.completed }.keys != snapshot.tasks.filterValues { it.completed }.keys) {
            delay(320)
        }
        displayed = snapshot
    }
    return displayed
}
