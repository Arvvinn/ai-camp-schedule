package cn.aurange.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun ScheduleApp(model: ScheduleViewModel, route: String?, onRouteHandled: () -> Unit,
                            permissionVersion: Int, askNotifications: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedTask by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val savedScreens = rememberSaveableStateHolder()
    val engine = model.engine
    val tasks = remember(state.snapshot) { engine.allTasks(state.snapshot).associateBy { it.id } }
    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) { snackbar.showSnackbar(state.message, duration = SnackbarDuration.Long); model.clearMessage() }
    }
    LaunchedEffect(route, state.loggedIn, state.snapshot.syncedAt) {
        if (route != null && state.loggedIn) {
            tab = 0
            if (route.isEmpty()) { selectedTask = null; onRouteHandled() }
            else if (tasks.containsKey(route)) { selectedTask = route; onRouteHandled() }
            else if (state.snapshot.syncedAt > 0 && !state.busy) {
                model.message("这条任务已不在当前排期中，已打开今日待办。"); onRouteHandled()
            }
        }
    }
    LaunchedEffect(state.loggedIn, state.demo) {
        if (!state.loggedIn && !state.demo) {
            selectedTask = null; tab = 0
            for (key in 0..2) savedScreens.removeState(key)
        }
    }
    BackHandler(enabled = selectedTask != null || tab != 0) {
        if (selectedTask != null) selectedTask = null else tab = 0
    }
    Scaffold(containerColor = Canvas,
        topBar = { AppHeader(state, model::refresh, model::signOut) },
        bottomBar = { if (state.loggedIn || state.demo) AppNavigation(tab) { tab = it } },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            AnimatedVisibility(state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Blue, trackColor = Lavender) }
            if (state.loggedIn || state.demo) {
                if (!state.demo && !state.online) OfflineBanner(state)
                Crossfade(tab, modifier = Modifier.weight(1f), animationSpec = tween(160), label = "page") { page ->
                    savedScreens.SaveableStateProvider(page) {
                        when (page) {
                            0 -> TodayScreen(state, engine, { selectedTask = it.id }, model::complete)
                            1 -> CampsScreen(state, engine, { selectedTask = it.id }, model::complete)
                            2 -> SettingsScreen(state, model, permissionVersion, askNotifications)
                        }
                    }
                }
            } else LoginScreen(state, model)
        }
    }
    selectedTask?.let { id -> tasks[id]?.let { task -> TaskDetail(task, state, model, onDismiss = { selectedTask = null }) } }
}

@Composable private fun AppHeader(state: AppState, refresh: () -> Unit, signOut: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 22.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AppMark()
        Text("营期日程", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
        when {
            state.demo -> {
                Text("演示预览", color = Muted, fontSize = 10.sp)
                TextButton(onClick = signOut, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("登录", fontSize = 12.sp) }
            }
            state.loggedIn -> {
                if (state.pendingCompletions.isNotEmpty()) Text("保存中 ${state.pendingCompletions.size}", color = Muted, fontSize = 10.sp)
                AppIcon(if (state.online) Glyph.Cloud else Glyph.Offline, modifier = Modifier.size(15.dp), tint = if (state.online) Sage else Amber)
                IconButton(onClick = refresh, enabled = !state.busy, modifier = Modifier.size(40.dp)) { AppIcon(Glyph.Refresh, "刷新排期", modifier = Modifier.size(18.dp), tint = Blue) }
            }
        }
    }
}

@Composable private fun OfflineBanner(state: AppState) {
    Row(Modifier.fillMaxWidth().background(phaseBackground("exam")).padding(horizontal = 22.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(Glyph.Offline, modifier = Modifier.size(13.dp), tint = Amber)
        val last = if (state.snapshot.syncedAt > 0) " · 上次同步 " + java.time.Instant.ofEpochMilli(state.snapshot.syncedAt)
            .atZone(BUSINESS_ZONE).format(java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm")) else "，请刷新重试"
        Text("离线缓存$last", color = Color(0xFF9C754A), fontSize = 10.sp)
    }
}

@Composable private fun AppNavigation(tab: Int, select: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth().background(Canvas).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 3.dp, border = BorderStroke(1.dp, Line.copy(alpha = .4f))) {
            Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val labels = listOf("今日", "营期", "设置")
                val icons = listOf(Glyph.Sun, Glyph.Layers, Glyph.Sliders)
                labels.forEachIndexed { index, title ->
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (tab == index) Lavender else Color.Transparent)
                        .selectable(tab == index, role = Role.Tab, onClick = { select(index) })
                        .heightIn(min = 46.dp).padding(horizontal = 6.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(icons[index], tint = if (tab == index) Blue else Muted, modifier = Modifier.size(19.dp))
                        Text(title, color = if (tab == index) Blue else Muted, fontSize = 12.sp, fontWeight = if (tab == index) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}
