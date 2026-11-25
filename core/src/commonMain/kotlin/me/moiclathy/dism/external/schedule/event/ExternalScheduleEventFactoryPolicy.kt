package me.moiclathy.dism.external.schedule.event

/**
 * 11.9 Event Factory 策略。
 */
data class ExternalScheduleEventFactoryPolicy(
    /**
     * 是否允许没有开始时间的 Event。
     */
    val allowUnscheduled: Boolean = true,

    /**
     * 是否允许没有结束时间的 Event。
     *
     * 这对于 At(start) Event 很重要。
     */
    val allowMissingEndTime: Boolean = true,

    /**
     * 是否保留外部 metadata。
     */
    val preserveMetadata: Boolean = true,

    /**
     * 是否要求 externalKey。
     */
    val requireExternalKey: Boolean = true,
)
