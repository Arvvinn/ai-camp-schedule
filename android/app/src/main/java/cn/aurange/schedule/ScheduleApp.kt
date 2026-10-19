package cn.aurange.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler

@Composable fun ScheduleApp(model: ScheduleViewModel, route: String?, onRouteHandled: () -> Unit,
                            permissionVersion: Int, askNotifications: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedTask by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
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
        if (!state.loggedIn && !state.demo) { selectedTask = null; tab = 0 }
    }
    BackHandler(enabled = selectedTask != null || tab != 0) {
        if (selectedTask != null) selectedTask = null else tab = 0
    }
    Scaffold(containerColor = Canvas,
        topBar = { AppHeader(state) },
        bottomBar = {
            if (state.loggedIn || state.demo) NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                val labels = listOf("今日", "营期", "设置")
                val icons = listOf(Icons.Default.Home, Icons.Default.DateRange, Icons.Default.Settings)
                labels.forEachIndexed { index, title ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = null) }, label = { Text(title) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFEFF6FF), selectedIconColor = Blue,
                            selectedTextColor = Blue, unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }, snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Blue, trackColor = Color(0xFFDBEAFE))
            if (state.loggedIn || state.demo) {
                SyncBanner(state, model::refresh, model::signOut)
                when (tab) {
                    0 -> TodayScreen(state, engine, { selectedTask = it.id }, model::complete)
                    1 -> CampsScreen(state, engine, { selectedTask = it.id }, model::complete)
                    2 -> SettingsScreen(state, model, permissionVersion, askNotifications)
                }
            } else LoginScreen(state, model)
        }
    }
    selectedTask?.let { id -> tasks[id]?.let { task ->
        TaskDetail(task, state, model, onDismiss = { selectedTask = null })
    } }
}

@Composable private fun AppHeader(state: AppState) {
    Surface(color = Navy) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("营期日程", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(state.today.format(fullDate), color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text("AI CAMP", color = Color(0xFF94A3B8), fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable private fun SyncBanner(state: AppState, refresh: () -> Unit, signOut: () -> Unit) {
    val background = if (state.demo || !state.online) Color(0xFFFFFBEB) else Color(0xFFEFF6FF)
    Row(Modifier.fillMaxWidth().background(background).padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(if (state.demo) "演示预览 · 示例数据" else if (state.online) "已与团队排期同步" else "离线缓存 · 等待同步",
                color = if (state.online) phaseColor("prep") else phaseColor("exam"), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (!state.demo && !state.online && state.snapshot.syncedAt > 0) Text("上次同步 " +
                java.time.Instant.ofEpochMilli(state.snapshot.syncedAt).atZone(BUSINESS_ZONE).format(java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm")),
                color = Muted, fontSize = 10.sp)
        }
        if (state.demo) TextButton(onClick = signOut) { Text("去登录", fontSize = 12.sp) }
        else IconButton(onClick = refresh, enabled = !state.busy) { Icon(Icons.Default.Refresh, contentDescription = "刷新排期", tint = Blue) }
    }
}

@Composable private fun LoginScreen(state: AppState, model: ScheduleViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val submit = { focus.clearFocus(); model.signIn(email, password); password = "" }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Spacer(Modifier.height(18.dp)) }
        item {
            Text("把每天的安排\n放在手边。", fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("查看营期进度，及时处理每一项待办。", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 12.dp))
        }
        item {
            WhiteCard {
                Text("登录团队账号", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("使用网站相同的邮箱和密码", color = Muted, fontSize = 12.sp)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("邮箱") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !state.busy, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (email.isNotBlank() && password.isNotEmpty() && !state.busy) submit() }))
                Button(onClick = submit, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp),
                    enabled = !state.busy && email.isNotBlank() && password.isNotEmpty()) {
                    Text(if (state.busy) "正在登录…" else "登录并同步排期")
                }
            }
        }
        item {
            TextButton(onClick = model::demo, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("先看看应用", fontSize = 13.sp) }
            Text("登录后可同步待办、开启每日提醒，并直接打开飞书资料。", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
        }
        item { TextButton(onClick = { ExternalLinks.document(context, "https://internal.aurange.cn/ai-camp-schedule/") }) { Text("打开网页版", fontSize = 12.sp) } }
    }
}
