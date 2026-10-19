package cn.aurange.schedule

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

class ScheduleEngine(val sop: SopData) {
    fun phases(camp: Camp): List<Phase> {
        val monday = camp.openDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val result = mutableListOf(
            Phase("prep", "开营筹备", "筹", "prep", camp.openDate.minusMonths(1), monday.minusDays(1)),
            Phase("open", "开营周", "开", "open", monday, monday.plusDays(6))
        )
        var start = monday.plusWeeks(1)
        for (n in 1..12) {
            if (n in camp.skips) {
                result += Phase("rest-$n", "休息周", "休", "rest", start, start.plusDays(6))
                start = start.plusWeeks(1)
            }
            val type = when (n) { 7 -> "exam"; 12 -> "close"; else -> "regular" }
            val label = when (n) { 7 -> "第7周 · 考试"; 12 -> "第12周 · 结营"; else -> "第${n}周" }
            result += Phase("week-$n", label, "$n", type, start, start.plusDays(6), n)
            start = start.plusWeeks(1)
        }
        return result
    }

    fun templates(phase: Phase): List<TaskTemplate> = when (phase.type) {
        "prep" -> sop.prep
        "open" -> sop.open
        "rest" -> sop.rest
        else -> sop.regular.filter { phase.week != 1 || !it.notW1 } +
            sop.extras[phase.week].orEmpty() + if (phase.week == 12) sop.closing else emptyList()
    }

    fun tasks(camp: Camp, phase: Phase): List<TaskOccurrence> = templates(phase).map { task ->
        val date = if (task.day == "all") phase.start else phase.start.plusDays(((task.day.toInt() + 6) % 7).toLong())
        val key = "${task.originalWeek ?: phase.key}|${task.originalDay ?: task.day}|${task.originalTitle ?: task.title}"
        TaskOccurrence(camp, phase, task, date, key)
    }

    fun allTasks(snapshot: Snapshot): List<TaskOccurrence> = snapshot.camps.flatMap { camp -> phases(camp).flatMap { tasks(camp, it) } }
    fun complete(snapshot: Snapshot, task: TaskOccurrence): Boolean = snapshot.tasks[task.id]?.completed == true
    fun current(camp: Camp, date: LocalDate): Phase? = phases(camp).firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }

    fun today(snapshot: Snapshot, date: LocalDate): List<TaskOccurrence> {
        return allTasks(snapshot).filter { task ->
            if (task.template.follow) !task.date.isAfter(date) && !complete(snapshot, task)
            else !date.isBefore(task.phase.start) && !date.isAfter(task.phase.end) &&
                (task.template.day == "all" || task.date == date || (task.date < date && !complete(snapshot, task)))
        }.distinctBy { it.id }.sortedWith(compareBy<TaskOccurrence>(
            { if (!it.template.follow && it.template.day != "all" && it.date < date) 0 else if (it.template.follow) 2 else 1 },
            { it.clockTime ?: java.time.LocalTime.MAX }, { it.camp.term }, { it.template.title }
        ))
    }
}
