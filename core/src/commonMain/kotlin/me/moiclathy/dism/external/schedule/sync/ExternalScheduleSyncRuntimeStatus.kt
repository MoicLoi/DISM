package me.moiclathy.dism.external.schedule.sync

/**
 * 一次完整 Sync Runtime 的状态。
 */
enum class ExternalScheduleSyncRuntimeStatus {

    /**
     * 所有需要执行的操作均成功。
     */
    COMPLETED,

    /**
     * 部分操作成功，部分失败。
     */
    PARTIAL,

    /**
     * 所有操作均失败。
     */
    FAILED,

    /**
     * 没有任何需要执行的操作。
     */
    NO_CHANGES,
}
