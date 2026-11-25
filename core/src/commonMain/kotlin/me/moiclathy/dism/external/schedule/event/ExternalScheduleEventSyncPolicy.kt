package me.moiclathy.dism.external.schedule.event

/**
 * Event Sync 策略。
 *
 * 这里控制的是：
 *
 * 外部 Schedule 的变化
 *        ↓
 * DISM Event 应如何处理
 *
 * 不负责决定 Diff。
 */
data class ExternalScheduleEventSyncPolicy(

    /**
     * 没有 Event ID 的 CHANGE 是否允许重新创建。
     *
     * 这是恢复场景的重要兜底。
     */
    val recreateMissingEventMapping: Boolean = true,

    /**
     * Removed Event 是否应该继续交给 Event 层。
     */
    val handleRemovedEvents: Boolean = true,

    /**
     * 是否允许创建 Event。
     */
    val allowCreate: Boolean = true,

    /**
     * 是否允许更新 Event。
     */
    val allowUpdate: Boolean = true,

    /**
     * 是否保留原有 Event ID。
     *
     * 正常情况下应当保持 true。
     */
    val preserveEventIdOnUpdate: Boolean = true,
)
