package cn.aurange.schedule

import java.time.LocalDate

data class CampDraft(val term: Int, val openDate: LocalDate, val skips: Set<Int> = emptySet()) {
    fun error(camps: List<Camp>): String? = when {
        term !in 1..10000 -> "期数请输入 1～10000 之间的整数。"
        camps.any { it.term == term } -> "第 $term 期已存在，请换一个期数。"
        openDate.year !in 2000..2100 -> "开营日期请选择 2000～2100 年之间。"
        skips.any { it !in 1..12 } -> "休息周应在第 1～12 周之间。"
        else -> null
    }
}
