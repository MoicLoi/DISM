package me.moiclathy.dism.external.schedule.sync.transaction

/**
 * 一次完整 Sync Run 的状态。
 */
enum class ExternalScheduleSyncRunStatus {

    /**
     * 已创建，但还没有开始执行。
     */
    CREATED,

    /**
     * 正在执行。
     */
    IN_PROGRESS,

    /**
     * 所有必要 Action 都成功完成。
     */
    COMPLETED,

    /**
     * 有 Action 失败，但 Run 仍然可以恢复。
     */
    PARTIAL,

    /**
     * Run 被明确判定为失败。
     */
    FAILED,

    /**
     * Run 已被 Recovery 接管。
     */
    RECOVERING,

    /**
     * Recovery 已完成。
     */
    RECOVERED,

    /**
     * 没有任何需要执行的 Action。
     */
    NO_CHANGES,
}
