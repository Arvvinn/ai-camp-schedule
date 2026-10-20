package cn.aurange.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

data class AppState(
    val snapshot: Snapshot = Snapshot(), val email: String = "", val loggedIn: Boolean = false,
    val busy: Boolean = false, val online: Boolean = false, val demo: Boolean = false,
    val message: String = "", val settings: ReminderSettings = ReminderSettings(),
    val custom: Map<String, Long> = emptyMap(), val today: LocalDate = LocalDate.now(BUSINESS_ZONE)
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    val repo = ScheduleRepository.get(application)
    val engine get() = repo.engine
    private val mutableState = MutableStateFlow(AppState())
    val state = mutableState.asStateFlow()
    private var active: Job? = null

    init {
        val session = repo.store.session()
        mutableState.value = AppState(repo.store.snapshot(), session?.email.orEmpty(), session != null,
            settings = repo.store.settings(), custom = repo.store.custom())
        if (repo.store.demo() && session == null) demo() else if (session != null) refresh()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val date = LocalDate.now(BUSINESS_ZONE)
                if (date != mutableState.value.today) mutableState.value = mutableState.value.copy(today = date)
            }
        }
    }
    private fun run(block: suspend () -> Snapshot) {
        if (active?.isActive == true) return
        mutableState.value = mutableState.value.copy(busy = true, message = "")
        active = viewModelScope.launch {
            try {
                val snapshot = block()
                val session = repo.store.session()
                mutableState.value = mutableState.value.copy(snapshot = snapshot, loggedIn = session != null,
                    email = session?.email.orEmpty(), online = true, demo = false, custom = repo.store.custom())
                SyncWorker.enable(getApplication())
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                val session = repo.store.session()
                mutableState.value = mutableState.value.copy(snapshot = repo.store.snapshot(), loggedIn = session != null,
                    email = session?.email.orEmpty(), online = false, message = friendlyError(error), custom = repo.store.custom())
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
                withContext(Dispatchers.IO) { ReminderScheduler(getApplication()).rebuild() }
            }
        }
    }
    fun signIn(email: String, password: String) = run { repo.signIn(email, password) }
    fun refresh() {
        val date = LocalDate.now(BUSINESS_ZONE)
        if (mutableState.value.today != date) mutableState.value = mutableState.value.copy(today = date)
        if (!mutableState.value.demo && mutableState.value.loggedIn) run { repo.refresh() }
    }
    fun complete(task: TaskOccurrence) {
        val state = mutableState.value
        if (!state.online || state.demo || state.busy) return
        run { repo.complete(task, !engine.complete(state.snapshot, task), state.snapshot.tasks[task.id]?.version ?: 0) }
    }
    fun demo() {
        if (active?.isActive == true) return
        val today = LocalDate.now(BUSINESS_ZONE)
        val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val sample = Snapshot(listOf(Camp("demo-1", 8, sunday.minusWeeks(6)), Camp("demo-2", 9, sunday.plusWeeks(2))))
        val completed = engine.today(sample, today)
            .filter { !it.template.follow && (it.template.day == "all" || it.date == today) }.take(2)
            .associate { it.id to TaskState(true, 1) }
        val snapshot = sample.copy(tasks = completed)
        repo.store.demo(true)
        mutableState.value = AppState(snapshot, demo = true, settings = repo.store.settings(), today = today)
    }
    fun signOut() {
        if (active?.isActive == true) return
        viewModelScope.launch {
            repo.signOut()
            withContext(Dispatchers.IO) { ReminderScheduler(getApplication()).cancel() }
            SyncWorker.disable(getApplication())
            mutableState.value = AppState(settings = repo.store.settings())
        }
    }
    fun settings(value: ReminderSettings) {
        repo.store.settings(value)
        mutableState.value = mutableState.value.copy(settings = value)
        viewModelScope.launch(Dispatchers.IO) { ReminderScheduler(getApplication()).rebuild() }
    }
    fun custom(taskId: String, at: Long?) {
        if (mutableState.value.demo) { message("演示模式不保存任务提醒；登录后可设置。"); return }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.store.custom(taskId, at)
                ReminderScheduler(getApplication()).rebuild()
            }
            mutableState.value = mutableState.value.copy(custom = repo.store.custom(), message = if (at == null) "已取消本机提醒。" else "已设置本机提醒。")
        }
    }
    fun message(value: String) { mutableState.value = mutableState.value.copy(message = value) }
    fun clearMessage() { message("") }
}
