package me.moiclathy.dism.external.schedule.sync.journal

/**
 * Sync Journal 中记录的事实类型。
 */
enum class ExternalScheduleSyncJournalEventType {

    /**
     * 一个 Sync Run 开始。
     */
    RUN_STARTED,

    /**
     * 一个 Action 开始。
     */
    ACTION_STARTED,

    /**
     * Event 已经成功创建。
     */
    EVENT_CREATED,

    /**
     * Event 已经成功更新。
     */
    EVENT_UPDATED,

    /**
     * Event removal 已经执行。
     */
    EVENT_REMOVAL_HANDLED,

    /**
     * External -> Event mapping 已提交。
     */
    MAPPING_COMMITTED,

    /**
     * 一个 Action 完成。
     */
    ACTION_COMPLETED,

    /**
     * 一个 Action 失败。
     */
    ACTION_FAILED,

    /**
     * 一个 Action 被跳过。
     */
    ACTION_SKIPPED,

    /**
     * Run 完成。
     */
    RUN_COMPLETED,

    /**
     * Run Recovery 开始。
     */
    RECOVERY_STARTED,

    /**
     * Run Recovery 完成。
     */
    RECOVERY_COMPLETED,

    /**
     * 没有对应 Event Action 的 Reconciliation 决策，
     * 例如 NO_OP 与 COMPLETE_SYNC。
     *
     * 11.32 新增。
     */
    RECONCILIATION_OBSERVED,
}
