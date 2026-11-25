package me.moiclathy.dism.schedule.snapshot

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

/**
 * 13.2：
 *
 * Snapshot 与 Trace 之间只保留 **弱关联**：
 *
 * ```text
 * Snapshot ──traceId──► Trace
 * ```
 *
 * Snapshot 不持有 Trace 对象，也不 import TraceRecord。
 * （原来这里有一个未使用的 TraceRecord import，已删除。）
 */
data class DynamicScheduleDecisionSnapshot(
    val id: DynamicScheduleDecisionSnapshotId =
        DynamicScheduleDecisionSnapshotId(),

    val itemId: DynamicScheduleItemId,

    val capturedAt: Instant,

    val scheduleRevision: Long,

    val engineVersion: String,

    val item:
        DynamicScheduleItemSnapshot,

    val context:
        DynamicScheduleContextSnapshot,

    val attentionMemory:
        DynamicScheduleAttentionMemorySnapshot,

    val dependency:
        DynamicScheduleDependencySnapshot,

    val policy:
        DynamicSchedulePolicySnapshot,

    val traceId:
        DynamicScheduleDecisionTraceId? = null,
) {
    init {
        require(scheduleRevision >= 0)
        require(engineVersion.isNotBlank())
    }
}
