package cn.aurange.schedule

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
import org.json.JSONArray

class ScheduleEngineTest {
    private val sop = NativeJson.sop(File(System.getProperty("schedule.assets"), "schedule.json").readText())
    private val engine = ScheduleEngine(sop)
    private fun camp(date: String = "2026-10-25", skips: Set<Int> = emptySet()) = Camp("camp-id", 1, LocalDate.parse(date), skips)

    @Test fun monthEndAndLeapYearPreparation() {
        assertEquals(LocalDate.parse("2027-02-28"), engine.phases(camp("2027-03-31"))[0].start)
        assertEquals(LocalDate.parse("2028-02-29"), engine.phases(camp("2028-03-31"))[0].start)
        val phases = engine.phases(camp())
        assertEquals(LocalDate.parse("2026-10-18"), phases[0].end)
        assertEquals("open", engine.current(camp(), LocalDate.parse("2026-10-19"))?.key)
    }
    @Test fun allSkipCombinationsKeepTwelveTeachingWeeksAndContinuousDates() {
        for (mask in 0 until 4096) {
            val skips = (1..12).filter { mask and (1 shl (it - 1)) != 0 }.toSet()
            val phases = engine.phases(camp(skips = skips))
            assertEquals(14 + skips.size, phases.size)
            assertEquals((1..12).toList(), phases.filter { it.week > 0 }.map { it.week })
            phases.zipWithNext().forEach { (a, b) -> assertEquals(a.end.plusDays(1), b.start) }
        }
    }
    @Test fun preservedCloudKeysAndWeekOneRules() {
        val c = camp(); val phases = engine.phases(c)
        val prep = engine.tasks(c, phases.first())
        assertEquals("open|all|创建本期课程", prep[1].key)
        assertEquals("open|all|更新本期云空间配套资料", prep[2].key)
        val open = engine.tasks(c, phases[1])
        assertTrue(open.any { it.key.startsWith("open|6|鹅圈子") && it.template.day == "0" })
        assertFalse(engine.templates(phases.first { it.week == 1 }).any { it.notW1 })
        assertTrue(engine.templates(phases.first { it.week == 2 }).any { it.notW1 })
        assertTrue(engine.templates(phases.last()).any { it.title == "提醒本周作业提交截止" })
        assertTrue(engine.templates(phases.last()).any { it.title == "结营典礼直播" })
        assertEquals("exam", phases.first { it.week == 7 }.type)
    }
    @Test fun followupsSurviveClosingAndCompletionSuppressesThem() {
        val c = camp(); val snapshot = Snapshot(listOf(c))
        val after = engine.phases(c).last().end.plusMonths(1)
        val follow = engine.today(snapshot, after)
        assertTrue(follow.isNotEmpty()); assertTrue(follow.all { it.template.follow })
        val complete = snapshot.copy(tasks = follow.associate { it.id to TaskState(true, 1) })
        assertTrue(engine.today(complete, after).isEmpty())
    }
    @Test fun overdueIsLimitedToCurrentWeekAndTaskIdsAreUnique() {
        val c = camp(); val snapshot = Snapshot(listOf(c))
        val date = LocalDate.parse("2026-11-05")
        val today = engine.today(snapshot, date)
        assertTrue(today.any { it.date < date && !it.template.follow && it.template.day != "all" })
        assertTrue(today.filterNot { it.template.follow }.all { it.phase.key == "week-2" })
        val all = engine.allTasks(snapshot)
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertEquals(snapshot, NativeJson.snapshot(NativeJson.snapshot(snapshot)))
    }
    @Test fun nativePhasesDatesAndEveryTaskKeyMatchActualWebsiteImplementation() {
        val cases = JSONArray(File(System.getProperty("schedule.webFixture")).readText()).objects()
        for (case in cases) {
            val skips = case.getJSONArray("skips")
            val c = camp(case.getString("openDate"), (0 until skips.length()).map { skips.getInt(it) }.toSet())
            val expected = case.getJSONArray("phases").objects()
            val phases = engine.phases(c)
            assertEquals(expected.size, phases.size)
            expected.zip(phases).forEach { (web, native) ->
                val label = "${c.openDate} ${c.skips} ${native.key}"
                assertEquals(label, web.getString("key"), native.key)
                assertEquals(label, web.getString("type"), native.type)
                assertEquals(label, web.getString("start"), native.start.toString())
                assertEquals(label, web.getString("end"), native.end.toString())
                val keys = web.getJSONArray("keys")
                assertEquals(label, (0 until keys.length()).map { keys.getString(it) }, engine.tasks(c, native).map { it.key })
            }
        }
    }
}
