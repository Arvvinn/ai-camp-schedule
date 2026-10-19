package cn.aurange.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TaskDetail(task: TaskOccurrence, state: AppState, model: ScheduleViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val completed = model.engine.complete(state.snapshot, task)
    val custom = state.custom[task.id]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = androidx.compose.ui.graphics.Color.White) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PhaseBadge(task.phase, "第${task.camp.term}期 · ${task.phase.label}")
            Text(task.template.title, fontSize = 23.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold)
            val dateLabel = if (task.template.day == "all") "全周 / 日常 · ${task.phase.start.format(shortDate)} — ${task.phase.end.format(shortDate)}"
                else task.date.format(fullDate)
            Text(dateLabel + if (task.template.time.isNotEmpty()) " · ${task.template.time}" else "", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            if (task.template.description.isNotEmpty()) Text(task.template.description, color = Navy, fontSize = 14.sp, lineHeight = 25.sp)
            if (task.template.follow) Text("持续跟进事项，完成后将从每日待办中移除。", color = phaseColor("prep"), fontSize = 12.sp, lineHeight = 20.sp)
            val links = task.template.links.mapNotNull { model.repo.sop.links[it] }
            if (links.isNotEmpty()) {
                HorizontalDivider(color = Line)
                Text("相关资料", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                for (link in links) OutlinedButton(onClick = {
                    if (!ExternalLinks.document(context, link.url)) model.message("无法打开链接，请检查手机是否安装了浏览器或飞书。")
                }, modifier = Modifier.fillMaxWidth()) {
                    val feishu = java.net.URI(link.url).host.orEmpty().endsWith(".feishu.cn")
                    Text((if (feishu) "在飞书打开 · " else "打开 · ") + link.text, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = Line)
            Text("本机提醒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(if (custom != null) "已设为 " + Instant.ofEpochMilli(custom).atZone(BUSINESS_ZONE).format(DateTimeFormatter.ofPattern("M月d日 HH:mm")) + "（北京时间）"
                else if (task.clockTime != null) "默认在任务时间前 ${state.settings.leadMinutes} 分钟提醒"
                else "此任务进入每日汇总，可自行设置具体提醒时刻。", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            OutlinedButton(onClick = {
                if (state.demo) { model.message("演示模式不保存提醒，请先登录。"); return@OutlinedButton }
                val initial = custom?.let { Instant.ofEpochMilli(it).atZone(BUSINESS_ZONE).toLocalDateTime() }
                    ?.takeIf { it.isAfter(LocalDateTime.now(BUSINESS_ZONE)) } ?: LocalDateTime.now(BUSINESS_ZONE).plusHours(1)
                DatePickerDialog(context, { _, year, month, day ->
                    TimePickerDialog(context, { _, hour, minute ->
                        val at = LocalDate.of(year, month + 1, day).atTime(hour, minute).atZone(BUSINESS_ZONE).toInstant().toEpochMilli()
                        if (at <= System.currentTimeMillis()) model.message("请选择未来的提醒时间。")
                        else { model.custom(task.id, at); onDismiss() }
                    }, initial.hour, initial.minute, true).show()
                }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
            }, enabled = !completed && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (custom == null) "设定提醒时间" else "修改提醒时间") }
            if (custom != null) TextButton(onClick = { model.custom(task.id, null) }, enabled = !state.busy) { Text("取消本机单独提醒") }
            if (!state.settings.enabled) Text("当前日程提醒已关闭，请在设置中开启。", color = phaseColor("exam"), fontSize = 12.sp)
            if (state.demo || !state.online) Text(if (state.demo) "演示模式仅供预览，不修改团队数据。" else "离线时可查看；联网同步后可标记完成。",
                color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            Button(onClick = { model.complete(task) }, enabled = state.online && !state.demo && !state.busy,
                modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(if (completed) "恢复为未完成" else "标记为已完成") }
        }
    }
}
