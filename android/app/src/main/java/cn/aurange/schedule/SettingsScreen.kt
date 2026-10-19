package cn.aurange.schedule

import android.app.TimePickerDialog
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

@Composable private fun SettingsAction(title: String, description: String, status: String = "", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Muted, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
        }
        Text(if (status.isEmpty()) "›" else "$status  ›", color = Blue, fontSize = 12.sp)
    }
}

@Composable fun SettingsScreen(state: AppState, model: ScheduleViewModel, permissionVersion: Int, askNotifications: () -> Unit) {
    val context = LocalContext.current
    val scheduler = remember { ReminderScheduler(context) }
    val notificationEnabled = remember(permissionVersion) { scheduler.canNotify() }
    val summaryEnabled = remember(permissionVersion) { scheduler.canNotify(ReminderScheduler.SUMMARY_CHANNEL) }
    val exactAllowed = remember(permissionVersion) { scheduler.exactAllowed() }
    val battery = remember(permissionVersion) { ExternalLinks.batteryUnrestricted(context) }
    var chooseLead by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("提醒设置", "让重要安排按时出现在通知栏。") }
        item {
            WhiteCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("日程提醒", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("每日汇总与任务时间提醒", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 5.dp))
                    }
                    Switch(checked = state.settings.enabled, onCheckedChange = { model.settings(state.settings.copy(enabled = it)) })
                }
                HorizontalDivider(color = Line)
                SettingsAction("每日汇总", "汇总当天待办、本周逾期与持续跟进", state.settings.summaryTime.toString()) {
                    TimePickerDialog(context, { _, hour, minute -> model.settings(state.settings.copy(summaryTime = LocalTime.of(hour, minute))) },
                        state.settings.summaryTime.hour, state.settings.summaryTime.minute, true).show()
                }
                SettingsAction("任务提前提醒", "仅用于有明确时刻的任务", if (state.settings.leadMinutes == 0) "准时" else "${state.settings.leadMinutes} 分钟") { chooseLead = true }
                Text("“直播前”“创建后立即”等安排会进入每日汇总，也可在任务详情中单独设定提醒。所有时间均为北京时间。",
                    fontSize = 11.sp, color = Muted, lineHeight = 19.sp)
            }
        }
        item { SectionTitle("通知与后台", "完成系统授权后，锁屏时也能接收日程提醒。") }
        item {
            WhiteCard {
                SettingsAction("通知权限", "允许应用发送任务通知", if (notificationEnabled) "已开启" else "去开启", askNotifications)
                SettingsAction("每日汇总通知", "单独管理汇总通知的声音和显示方式", if (summaryEnabled) "已开启" else "去开启") {
                    ExternalLinks.notifications(context)
                }
                SettingsAction("精准提醒", if (exactAllowed) "已允许按计划时刻触发" else "未授权时按系统允许的时间提醒，可能延迟",
                    if (exactAllowed) "已允许" else "去授权") { ExternalLinks.exactAlarms(context) }
                HorizontalDivider(color = Line)
                SettingsAction("小米后台自启动", "在系统列表中找到「营期日程」并开启自启动；入口位置因系统版本而异") {
                    ExternalLinks.autostart(context)
                    model.message("若未进入自启动列表：设置 → 应用设置 → 权限管理 → 自启动管理，开启「营期日程」。")
                }
                SettingsAction("电池与后台运行", "小米手机可在应用详情的省电策略中选择「无限制」", if (battery) "无限制" else "查看") {
                    ExternalLinks.battery(context)
                }
                OutlinedButton(onClick = {
                    if (!scheduler.test()) { model.message("请先开启任务通知权限，并检查系统通知类别。" ); askNotifications() }
                    else model.message("测试通知已发送，请查看通知栏。")
                }, modifier = Modifier.fillMaxWidth()) { Text("发送一条测试通知") }
            }
        }
        item {
            WhiteCard {
                Text("同步与离线", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("已同步的日程可以离线查看和提醒。云端排期变动需联网同步后生效；后台同步由系统安排，打开应用可立即刷新。",
                    color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
                Text("重启手机后会恢复提醒。若在系统中强行停止应用，请重新打开一次。",
                    color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
                if (!state.demo) Text(state.email, color = Muted, fontSize = 12.sp)
                TextButton(onClick = { confirmLogout = true }, enabled = !state.busy) {
                    Text(if (state.demo) "退出演示，登录账号" else "退出登录", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item { Text("营期日程 ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE}", fontSize = 10.sp, color = Muted,
            modifier = Modifier.padding(vertical = 8.dp)) }
    }
    if (chooseLead) AlertDialog(onDismissRequest = { chooseLead = false }, title = { Text("任务提前提醒") }, text = {
        Column {
            for (minutes in listOf(0, 5, 15, 30, 60)) Row(Modifier.fillMaxWidth().clickable {
                model.settings(state.settings.copy(leadMinutes = minutes)); chooseLead = false
            }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.settings.leadMinutes == minutes, onClick = {
                    model.settings(state.settings.copy(leadMinutes = minutes)); chooseLead = false
                })
                Text(if (minutes == 0) "按任务时间准时提醒" else "提前 $minutes 分钟")
            }
        }
    }, confirmButton = { TextButton(onClick = { chooseLead = false }) { Text("取消") } })
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text(if (state.demo) "退出演示？" else "退出登录？") },
        text = { Text("退出后清除本机排期缓存和待发提醒。团队中的排期和完成记录会保留。") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; model.signOut() }) { Text("退出") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } })
}
