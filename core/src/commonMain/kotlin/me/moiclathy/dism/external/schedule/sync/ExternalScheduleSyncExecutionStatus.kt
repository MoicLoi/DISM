package me.moiclathy.dism.external.schedule.sync

/**
 * 单条 Sync Action 的执行状态。
 */
enum class ExternalScheduleSyncExecutionStatus {

    /**
     * 已创建 Event。
     */
    CREATED,

    /**
     * 已更新 Event。
     */
    UPDATED,

    /**
     * 已处理 Removed Event。
     */
    REMOVED,

    /**
     * 无需执行。
     */
    SKIPPED,

    /**
     * 执行失败。
     */
    FAILED,
}
