package me.moiclathy.dism.schedule.adaptation.provenance

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

/**
 * 14.14 / 14.15
 *
 * 一次 Dynamic Schedule Decision 的完整 Provenance。
 *
 * Provenance 不重新保存领域状态，
 * 而是把已经存在的 Snapshot / Trace / Replay /
 * Adaptation 等事实组织成可追溯的因果链。
 *
 * ### 它是**派生视图**
 *
 * ```text
 * Snapshot + Trace + Adaptation + Replay + Verification
 *     ↓
 * Provenance（每次需要时重新算）
 * ```
 *
 * 因此当前阶段**不单独持久化**。
 *
 * ### 为什么包名是 `adaptation.provenance`
 *
 * 项目里已经有 13.12 的
 * `me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenance`
 * （决策来源链，带 Store 与持久化）。
 *
 * 14.x 这一套是同名的**事后可追溯视图**，语义不同，
 * 所以规格把它放在 `schedule.adaptation.provenance` 子包下。
 * 两个包各自持有自己的同名类型，互不覆盖。
 */
data class DynamicScheduleDecisionProvenance(
    val itemId:
        DynamicScheduleItemId,

    val generatedAt:
        Instant,

    val snapshotId:
        DynamicScheduleDecisionSnapshotId? = null,

    val traceId:
        DynamicScheduleDecisionTraceId? = null,

    val engineVersion:
        String,

    val nodes:
        List<DynamicScheduleDecisionProvenanceNode>,

    /**
     * 最终 Provenance 的根节点。
     */
    val rootNodeId:
        String,

    /**
     * 最终输出节点。
     */
    val outputNodeId:
        String,

    /**
     * 这次 Provenance 走到哪一步。
     */
    val completeness:
        DynamicScheduleDecisionProvenanceCompleteness,

    val diagnostics:
        List<String> = emptyList(),
) {

    init {
        require(engineVersion.isNotBlank())
        require(nodes.isNotEmpty())

        val ids =
            nodes.map { it.id }

        require(ids.distinct().size == ids.size)
        require(ids.contains(rootNodeId))
        require(ids.contains(outputNodeId))
    }

    /**
     * 只有 [DynamicScheduleDecisionProvenanceCompleteness.COMPLETE]
     * 才算完整。
     */
    val complete: Boolean
        get() =
            completeness ==
                DynamicScheduleDecisionProvenanceCompleteness
                    .COMPLETE

    val decisiveNodes:
        List<DynamicScheduleDecisionProvenanceNode>
        get() =
            nodes.filter {
                it.decisive
            }

    val nodeCount: Int
        get() =
            nodes.size

    /**
     * 某个节点类型是否出现在这次 Provenance 里。
     */
    fun contains(
        type: DynamicScheduleDecisionProvenanceNodeType,
    ): Boolean =
        nodes.any { it.type == type }

    fun find(
        nodeId: String,
    ): DynamicScheduleDecisionProvenanceNode? =
        nodes.firstOrNull { it.id == nodeId }

    /**
     * 按 parentIds 沿着因果链向上回溯。
     *
     * 从 [outputNodeId] 出发就能得到
     * "这个决策是由哪些事实一步步形成的"。
     */
    fun ancestorsOf(
        nodeId: String,
    ): List<DynamicScheduleDecisionProvenanceNode> {

        val byId =
            nodes.associateBy { it.id }

        val visited =
            linkedSetOf<String>()

        val queue =
            ArrayDeque<String>()

        byId[nodeId]?.parentIds?.forEach { queue.addLast(it) }

        while (queue.isNotEmpty()) {

            val current =
                queue.removeFirst()

            if (!visited.add(current)) {
                continue
            }

            byId[current]?.parentIds?.forEach {
                if (it !in visited) {
                    queue.addLast(it)
                }
            }
        }

        return visited.mapNotNull { byId[it] }
    }
}
