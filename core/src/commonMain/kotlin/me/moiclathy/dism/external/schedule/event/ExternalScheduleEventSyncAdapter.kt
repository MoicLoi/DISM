package me.moiclathy.dism.external.schedule.event

import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncChange

/**
 * External Schedule → DISM Event 的同步适配边界。
 *
 * 该接口不关心：
 *
 * - SQLDelight
 * - Attention
 * - Context
 * - Time-flow
 *
 * 只负责把 Sync Change 转换成内部 Event 操作。
 */
interface ExternalScheduleEventSyncAdapter {

    /**
     * 根据外部记录创建 Event。
     *
     * @return 创建成功后的 Event ID。
     */
    fun create(
        change: ExternalScheduleSyncChange,
    ): String?

    /**
     * 根据外部记录更新已有 Event。
     *
     * @return 更新后的 Event ID。
     */
    fun update(
        change: ExternalScheduleSyncChange,
    ): String?

    /**
     * 处理外部记录已经被移除。
     *
     * @return 被处理的 Event ID。
     */
    fun handleRemoved(
        change: ExternalScheduleSyncChange,
    ): String?
}
