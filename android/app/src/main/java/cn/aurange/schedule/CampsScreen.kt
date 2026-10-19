package cn.aurange.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun CampsScreen(state: AppState, engine: ScheduleEngine, onTask: (TaskOccurrence) -> Unit,
                            onComplete: (TaskOccurrence) -> Unit) {
    var selectedCamp by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPhase by rememberSaveable { mutableStateOf<String?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val camps = remember(state.snapshot.camps, state.today) {
        state.snapshot.camps.sortedWith(compareBy<Camp>({ if (engine.current(it, state.today) != null) 0 else if (it.openDate > state.today) 1 else 2 }, { it.openDate }))
    }
    LaunchedEffect(camps) {
        if (!initialized && camps.isNotEmpty()) { selectedCamp = camps.first().id; initialized = true }
        if (selectedCamp != null && camps.none { it.id == selectedCamp }) selectedCamp = null
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("营期概览", "筹备、开营到结营，进度一目了然。") {
            TextButton(onClick = { ExternalLinks.document(context, "https://internal.aurange.cn/ai-camp-schedule/") }) { Text("管理营期", fontSize = 11.sp) }
        } }
        if (camps.isEmpty()) item { EmptyCard("暂无团队营期", "在网页版添加营期后，回到应用刷新即可同步。") }
        for (camp in camps) {
            val phases = engine.phases(camp)
            val current = engine.current(camp, state.today)
            val expanded = selectedCamp == camp.id
            val phase = phases.firstOrNull { it.key == selectedPhase && expanded } ?: current ?: if (state.today < phases.first().start) phases.first() else phases.last()
            val tasks = engine.tasks(camp, phase)
            val done = tasks.count { engine.complete(state.snapshot, it) }
            item(key = "${camp.id}-header") {
                WhiteCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("第 ${camp.term} 期", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(camp.openDate.format(shortDate) + " 开营 · " + phases.last().end.format(shortDate) + " 结营",
                                color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        PhaseBadge(current, if (current == null) { if (state.today < phases.first().start) "未开始" else "已结营" } else null)
                    }
                    if (camp.skips.isNotEmpty()) Text("长假顺延 ${camp.skips.size} 周", color = Muted, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (expanded) phase.label else "共 12 个课程周", fontSize = 12.sp, color = Muted, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selectedCamp = if (expanded) null else camp.id; selectedPhase = null }) {
                            Text(if (expanded) "收起" else "查看安排", fontSize = 12.sp)
                        }
                    }
                    if (expanded) {
                        LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
                            modifier = Modifier.fillMaxWidth().height(5.dp), color = phaseColor(phase.type), trackColor = phaseBackground(phase.type))
                        Text("本阶段已完成 $done / ${tasks.size}", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            if (expanded) {
                item(key = "${camp.id}-phases") {
                    val phaseIndex = phases.indexOf(phase).coerceAtLeast(0)
                    val phaseScroll = rememberLazyListState(initialFirstVisibleItemIndex = (phaseIndex - 2).coerceAtLeast(0))
                    LaunchedEffect(phase.key) { phaseScroll.animateScrollToItem((phaseIndex - 2).coerceAtLeast(0)) }
                    LazyRow(state = phaseScroll, horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                        items(phases, key = { it.key }) { p ->
                            FilterChip(selected = phase.key == p.key, onClick = { selectedPhase = p.key }, label = { Text(p.short) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = phaseBackground(p.type),
                                    selectedLabelColor = phaseColor(p.type), containerColor = Color.White),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = phase.key == p.key,
                                    borderColor = Line, selectedBorderColor = phaseColor(p.type)))
                        }
                    }
                }
                item(key = "${camp.id}-range") {
                    Text("${phase.label}  ·  ${phase.start.format(shortDate)} — ${phase.end.format(shortDate)}",
                        color = phaseColor(phase.type), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
                }
                for (day in listOf("all", "1", "2", "3", "4", "5", "6", "0")) {
                    val dayTasks = tasks.filter { it.template.day == day }
                    if (dayTasks.isEmpty()) continue
                    item(key = "${camp.id}-${phase.key}-$day-label") {
                        val label = if (day == "all") "全周 / 日常" else dayTasks.first().date.let { it.format(weekDay) + " " + it.format(shortDate) }
                        Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                    }
                    items(dayTasks, key = { "camp:${it.id}" }) { task ->
                        Surface(color = Color.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line)) {
                            Box(Modifier.padding(horizontal = 12.dp)) {
                                TaskRow(task, engine.complete(state.snapshot, task), state.online && !state.busy && !state.demo,
                                    isOverdue(task, state.today), { onTask(task) }, { onComplete(task) })
                            }
                        }
                    }
                }
                item(key = "${camp.id}-spacer") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
