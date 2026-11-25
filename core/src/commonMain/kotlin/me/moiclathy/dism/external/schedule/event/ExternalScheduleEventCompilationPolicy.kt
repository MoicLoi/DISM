package me.moiclathy.dism.external.schedule.event

/**
 * 外部日程 → Event 编译策略。
 *
 * 这一层只决定：
 *
 * 1. 什么数据可以进入 Event 编译阶段
 * 2. 缺失字段如何处理
 * 3. metadata 如何保留
 *
 * 不负责：
 * - NLP
 * - Entity Resolution
 * - Recommendation
 * - Attention
 * - Event lifecycle
 */
data class ExternalScheduleEventCompilationPolicy(
    /**
     * 没有标题时是否允许使用外部 ID 作为标题。
     */
    val allowFallbackTitle: Boolean = true,

    /**
     * 没有开始时间时是否允许生成 UNSCHEDULED Event。
     */
    val allowUnscheduled: Boolean = true,

    /**
     * 是否保留原始 metadata。
     */
    val preserveMetadata: Boolean = true,

    /**
     * 是否拒绝结束时间早于开始时间。
     */
    val rejectInvalidTimeRange: Boolean = true,

    /**
     * 是否拒绝空标题。
     */
    val rejectBlankTitle: Boolean = true,
)
