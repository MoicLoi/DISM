package me.moiclathy.dism.external.schedule.persistence

/**
 * 外部课程记录恢复状态。
 */
enum class ExternalScheduleRecoveryStatus {

    /**
     * 记录完整，可以继续使用。
     */
    RESTORED,

    /**
     * 记录存在，但缺少 Event 映射。
     *
     * 后续可以重新进行 Event Compilation。
     */
    MISSING_EVENT_MAPPING,

    /**
     * 外部记录已经被上一轮 Import 标记为 inactive。
     */
    INACTIVE,

    /**
     * 数据存在，但无法继续安全使用。
     */
    INVALID,
}
