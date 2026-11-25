package me.moiclathy.dism.schedule.adaptation.provenance

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 14.14 / 14.16
 *
 * Provenance 的结构化查询。
 *
 * ### 为什么强制要求 itemId 或 snapshotId
 *
 * "查全部 Provenance"在生产环境里没有意义：
 *
 * ```text
 * 没有任何指向的查询 == 一次性把所有历史图都算出来
 * ```
 *
 * 那是一个会随历史增长而无上限变慢的操作。
 * 与其让它悄悄跑，不如在构造时就拒绝。
 */
data class DynamicScheduleDecisionProvenanceQuery(
    val itemId:
        DynamicScheduleItemId? = null,

    val snapshotId:
        DynamicScheduleDecisionSnapshotId? = null,

    /**
     * `true`：返回整个 Provenance。
     *
     * `false`：只返回 decisive nodes。
     */
    val includeNonDecisive:
        Boolean = true,

    /**
     * 是否包含诊断信息。
     */
    val includeDiagnostics:
        Boolean = true,
) {

    init {
        require(
            itemId != null || snapshotId != null,
        ) {
            "Either itemId or snapshotId must be provided."
        }
    }
}
