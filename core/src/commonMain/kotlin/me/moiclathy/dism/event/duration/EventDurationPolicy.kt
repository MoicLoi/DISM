package me.moiclathy.dism.event.duration

/**
 * Duration 计算策略。
 */
data class EventDurationPolicy(

    /**
     * 是否允许 estimated duration。
     */
    val allowEstimated: Boolean =
        true,

    /**
     * 是否允许 derived duration。
     */
    val allowDerived: Boolean =
        true,

    /**
     * 是否允许 zero duration。
     */
    val allowZeroDuration: Boolean =
        true,
)
