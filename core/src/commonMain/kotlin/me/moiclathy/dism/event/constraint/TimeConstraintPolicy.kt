package me.moiclathy.dism.event.constraint

/**
 * Time Constraint 计算策略。
 */
data class TimeConstraintPolicy(

    /**
     * 边界时间是否包含在合法范围内。
     *
     * 例如：
     *
     * latestEnd = 11:30
     *
     * evaluatedEnd = 11:30
     *
     * 默认认为合法。
     */
    val inclusiveBoundaries: Boolean = true,
)
