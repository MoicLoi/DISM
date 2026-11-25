package me.moiclathy.dism.external.schedule.sync

/**
 * 实际 Event 操作的适配边界。
 *
 * 11.15 Runtime 只负责：
 *
 * SyncPlan
 *     ↓
 * Handler
 *
 * 具体 Event 如何创建、更新、移除，
 * 由外部注入。
 */
interface ExternalScheduleSyncHandler {

    /**
     * 创建 Event。
     *
     * @return 创建出的内部 Event ID。
     */
    fun create(
        change: ExternalScheduleSyncChange,
    ): String?

    /**
     * 更新 Event。
     *
     * @return 更新后的内部 Event ID。
     */
    fun update(
        change: ExternalScheduleSyncChange,
    ): String?

    /**
     * 处理外部记录已经消失的情况。
     *
     * 不强制要求物理删除。
     */
    fun handleRemoved(
        change: ExternalScheduleSyncChange,
    ): String?
}
