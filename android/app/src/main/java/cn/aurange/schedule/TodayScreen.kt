package cn.aurange.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TodayScreen(state: AppState, engine: ScheduleEngine, onTask: (TaskOccurrence) -> Unit,
                            onComplete: (TaskOccurrence) -> Unit) {
    var selectedDate by rememberSaveable(state.today.toString()) { mutableStateOf(state.today.toString()) }
    var showCalendar by remember { mutableStateOf(false) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    val date = LocalDate.parse(selectedDate)
    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val tasks = remember(state.snapshot, date) { engine.today(state.snapshot, date) }
    val unfinished = tasks.count { !engine.complete(state.snapshot, it) }
    val completed = tasks.size - unfinished
    val visible = tasks.filter { showCompleted || !engine.complete(state.snapshot, it) }
    val canEdit = state.online && !state.demo && !state.busy

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle(if (date == state.today) "今天的安排" else date.format(shortDate) + "的安排",
                "按营期推进，把重要的事逐项完成。") {
                TextButton(onClick = { showCalendar = true }) { Text("选日期", fontSize = 12.sp) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (n in 0L..6L) {
                    val day = monday.plusDays(n)
                    val selected = day == date
                    Surface(Modifier.weight(1f).clickable { selectedDate = day.toString() },
                        color = if (selected) Navy else Color.White, shape = RoundedCornerShape(12.dp),
                        border = if (selected) null else BorderStroke(1.dp, Line)) {
                        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.format(weekDay).replace("周", ""), color = if (selected) Color(0xFFCBD5E1) else Muted, fontSize = 11.sp)
                            Text("${day.dayOfMonth}", color = if (selected) Color.White else Ink, fontSize = 17.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                            Text(if (day == state.today) "今天" else " ", color = if (selected) Color(0xFFFBBF24) else Blue,
                                fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
        }
        item {
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFDE68A))) {
                Column {
                    Surface(color = Amber, modifier = Modifier.fillMaxWidth().height(3.dp)) {}
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("待处理", fontSize = 12.sp, color = Muted)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$unfinished", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("项", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("已完成 $completed / ${tasks.size}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = phaseColor("regular"))
                            Text("持续跟进 ${tasks.count { it.template.follow }} 项", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("待办清单", if (date == state.today) "含本周逾期和持续跟进事项" else "按当前完成状态预览该日日程") {
                TextButton(onClick = { showCompleted = !showCompleted }) { Text(if (showCompleted) "隐藏已完成" else "显示已完成", fontSize = 11.sp) }
            }
        }
        if (state.snapshot.camps.isEmpty()) item {
            EmptyCard("还没有营期", "登录后同步团队排期；可以在网页版添加或管理营期。")
        } else if (visible.isEmpty()) item {
            EmptyCard(if (tasks.isNotEmpty()) "待办已全部完成" else "这一天暂无待办", "可到「营期」查看各周安排与相关资料。")
        }
        items(visible, key = { it.id }) { task ->
            Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(start = 8.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PhaseBadge(task.phase)
                        if (isOverdue(task, date)) Text(task.date.format(shortDate), fontSize = 11.sp, color = Muted,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    TaskRow(task, engine.complete(state.snapshot, task), canEdit, isOverdue(task, date),
                        { onTask(task) }, { onComplete(task) })
                }
            }
        }
        item { Text("提醒以北京时间为准 · 模糊时间任务可在详情中单独设提醒", color = Muted, fontSize = 10.sp,
            lineHeight = 17.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }
    if (showCalendar) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showCalendar = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let {
                selectedDate = java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
            }; showCalendar = false }) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { selectedDate = state.today.toString(); showCalendar = false }) { Text("回到今天") } }) {
            DatePicker(picker, title = { Text("选择日程日期", modifier = Modifier.padding(24.dp)) }, showModeToggle = false)
        }
    }
}
