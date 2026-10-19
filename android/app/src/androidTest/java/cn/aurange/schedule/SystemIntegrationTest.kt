package cn.aurange.schedule

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SystemIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo get() = ScheduleRepository.get(context)
    private fun fixture(): Pair<Snapshot, TaskOccurrence> {
        val today = LocalDate.now(BUSINESS_ZONE)
        val snapshot = Snapshot(listOf(Camp("instrumentation-camp", 999, today.plusDays(14))), syncedAt = System.currentTimeMillis())
        val task = repo.engine.allTasks(snapshot).first { it.template.follow }
        return snapshot to task
    }
    private fun loginFixture() {
        repo.store.clearAccount()
        repo.store.session(Session("instrumentation-only-access", "instrumentation-only-refresh",
            System.currentTimeMillis() / 1000 + 3600, "instrumentation@example.invalid", "instrumentation-user"))
        repo.store.settings(ReminderSettings())
    }

    @Test fun encryptedSessionAndCacheRoundTripAndLogoutClearsReminders() {
        loginFixture()
        try {
            val (snapshot, task) = fixture()
            repo.store.snapshot(snapshot)
            repo.store.custom(task.id, System.currentTimeMillis() + 60_000)
            val again = LocalStore(context)
            assertEquals("instrumentation-only-access", again.session()?.accessToken)
            val raw = context.getSharedPreferences("schedule-private", Context.MODE_PRIVATE).getString("session", "")!!
            assertFalse(raw.contains("instrumentation-only-access"))
            assertFalse(raw.contains("instrumentation-only-refresh"))
            assertEquals(snapshot, again.snapshot())
            assertNotNull(again.custom()[task.id])
            repo.store.clearAccount()
            ReminderScheduler(context).rebuild()
            assertNull(again.session()); assertTrue(again.snapshot().camps.isEmpty())
            assertTrue(again.custom().isEmpty()); assertNull(again.batch())
        } finally { repo.store.clearAccount(); ReminderScheduler(context).cancel() }
    }

    @Test fun completedTaskIsSuppressedAtDeliveryEvenWhenOldAlarmStillExists() {
        loginFixture()
        try {
            val (snapshot, task) = fixture()
            val now = System.currentTimeMillis()
            repo.store.snapshot(snapshot.copy(tasks = mapOf(task.id to TaskState(true, 1))))
            repo.store.batch(AlarmBatch(now - 100, listOf(ReminderEvent("completed-fixture", now - 100, task.id))))
            ReminderScheduler(context).deliverAndRebuild(now - 100)
            assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
            assertTrue(repo.store.delivered().containsKey("completed-fixture"))
        } finally { repo.store.clearAccount(); ReminderScheduler(context).cancel() }
    }

    @Test fun pendingNotificationRoutesToRealActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scheduler = ReminderScheduler(context)
        scheduler.channels()
        assertTrue("Grant POST_NOTIFICATIONS before running device checks", scheduler.canNotify())
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            assertTrue(scheduler.test())
            val manager = context.getSystemService(NotificationManager::class.java)
            val deadline = System.currentTimeMillis() + 5000
            while (manager.activeNotifications.none { it.id == 199 } && System.currentTimeMillis() < deadline) Thread.sleep(50)
            val notification = manager.activeNotifications.first { it.id == 199 }.notification
            assertEquals(context.packageName, notification.contentIntent.creatorPackage)
            assertTrue(notification.extras.getString("android.title").orEmpty().contains("通知测试"))
            notification.contentIntent.send()
            val activity = instrumentation.waitForMonitorWithTimeout(monitor, 5000)
            assertNotNull("Notification must open MainActivity", activity)
            assertTrue(activity.intent.hasExtra("taskId"))
            instrumentation.runOnMainSync { activity.finish() }
        } finally { instrumentation.removeMonitor(monitor); scheduler.cancel() }
    }

    // Used by the external ADB lifecycle check. No cloud call is made with this fixture.
    @Test fun prepareBackgroundAlarm() {
        loginFixture()
        val (snapshot, task) = fixture()
        repo.store.snapshot(snapshot)
        val now = System.currentTimeMillis()
        repo.store.custom(task.id, now + 30_000)
        val scheduler = ReminderScheduler(context)
        assertTrue(scheduler.exactAllowed())
        assertTrue(scheduler.canNotify())
        scheduler.rebuild()
        assertEquals(task.id, repo.store.batch()?.events?.single()?.taskId)
        assertTrue(repo.store.batch()!!.at > now)
    }

    @Test fun clearTestFixture() {
        repo.store.clearAccount()
        ReminderScheduler(context).cancel()
        assertNull(repo.store.session())
    }
}
