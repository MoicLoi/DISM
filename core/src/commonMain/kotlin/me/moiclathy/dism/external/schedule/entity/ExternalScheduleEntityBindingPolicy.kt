package me.moiclathy.dism.external.schedule.entity

/**
 * Entity Binding 策略。
 */
data class ExternalScheduleEntityBindingPolicy(
    val bindCourse: Boolean = true,
    val bindInstructor: Boolean = true,
    val bindLocation: Boolean = true,

    /**
     * 是否允许使用 EntityAlias。
     *
     * 11.7 默认开启，
     * 但只接受唯一、确定的 Alias。
     */
    val useAliases: Boolean = true,

    /**
     * 是否允许自动创建 Entity。
     *
     * 默认关闭。
     *
     * Entity 的创建应该由上层明确决定，
     * 而不是 Schedule Import 隐式产生大量 Entity。
     */
    val allowEntityCreation: Boolean = false,
)
