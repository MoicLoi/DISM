package me.moiclathy.dism.event.state

/**
 * Event State Transition 策略。
 */
data class EventStateTransitionPolicy(
    /**
     * 是否允许输出 PERSISTING Transition。
     *
     * 默认关闭。
     */
    val emitPersisting: Boolean = false,

    /**
     * 是否允许从 COMPLETED 返回其他状态。
     *
     * 默认关闭。
     *
     * 这样可以避免：
     *
     * COMPLETED → PLANNED
     *
     * 这种普通流程中通常不合理的转换。
     */
    val allowCompletedReopen: Boolean = false,

    /**
     * 是否允许从 CANCELLED 恢复。
     */
    val allowCancelledReopen: Boolean = false,

    /**
     * 是否允许 EXPIRED → PLANNED。
     *
     * 对延期/重新安排场景有用。
     */
    val allowExpiredReopen: Boolean = true,
)
