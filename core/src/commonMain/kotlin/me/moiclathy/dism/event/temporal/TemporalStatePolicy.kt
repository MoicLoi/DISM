package me.moiclathy.dism.event.temporal

/**
 * Temporal State 计算策略。
 */
data class TemporalStatePolicy(

    /**
     * Deadline 在恰好到达 deadline 时，
     * 是否认为处于 DUE。
     *
     * true:
     *
     * now == deadline
     *        ↓
     *      DUE
     */
    val deadlineAtIsDue: Boolean = true,
)
