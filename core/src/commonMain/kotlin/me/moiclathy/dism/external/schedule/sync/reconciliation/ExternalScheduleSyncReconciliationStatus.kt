package me.moiclathy.dism.external.schedule.sync.reconciliation

/**
 * 一条 Sync Action 当前实际状态。
 */
enum class ExternalScheduleSyncReconciliationStatus {

    /**
     * 状态与 Event 层一致。
     */
CONSISTENT,

    /**
     * Sync 状态已经完成，但 Event mapping 不存在。
     */
MISSING_EVENT,

    /**
     * Sync 记录认为正在执行，但 Event 已经存在。
     *
     * 这是典型的：
     *
     * Event 创建成功
     * ↓
     * complete() 尚未提交
     * ↓
     * APP 崩溃
     */
EVENT_EXISTS_BUT_SYNC_INCOMPLETE,

    /**
     * Sync 正在执行，但 Event 不存在。
     */
SYNC_INCOMPLETE_EVENT_MISSING,

    /**
     * Sync 失败，但 Event 已经存在。
     */
FAILED_BUT_EVENT_EXISTS,

    /**
     * Sync 失败，同时 Event 不存在。
     */
FAILED_AND_EVENT_MISSING,

    /**
     * 当前状态无法判断。
     */
UNKNOWN,
}
