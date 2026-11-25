package me.moiclathy.dism.external.schedule.sync.journal

/**
 * 当前 Action 的 Checkpoint 状态。
 */
enum class ExternalScheduleSyncCheckpointStatus {

    /**
     * 尚未开始。
     */
    NOT_STARTED,

    /**
     * Action 已开始。
     */
    STARTED,

    /**
     * Event 已创建。
     */
    EVENT_CREATED,

    /**
     * Event 已更新。
     */
    EVENT_UPDATED,

    /**
     * Mapping 已提交。
     */
    MAPPING_COMMITTED,

    /**
     * Action 已完成。
     */
    COMPLETED,

    /**
     * Action 被跳过。
     */
    SKIPPED,

    /**
     * Action 失败。
     */
    FAILED,
}
