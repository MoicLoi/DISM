package me.moiclathy.dism.schedule.adaptation.provenance

import kotlin.time.Instant

import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 14.14 / 14.15
 *
 * ```text
 * Snapshot + Trace + Adaptation + Boundary + Override + Replay/Verification
 *     → Provenance
 * ```
 */
interface DynamicScheduleDecisionProvenanceBuilder {

    fun build(
        input: DynamicScheduleDecisionProvenanceInput,
    ): DynamicScheduleDecisionProvenance

    /**
     * 14.14 / 14.15 规格里的五参数形态。
     *
     * 保留它是因为：只关心 Snapshot + Trace + Adaptation 的调用方
     * 不应该被迫构造一个带一堆 null 的 Input。
     *
     * 它内部**委托**给 [build] 的完整形态，
     * 所以两条路径永远得到同样的结果。
     */
    fun build(
        snapshot:
            DynamicScheduleDecisionSnapshot,

        trace:
            DynamicScheduleDecisionTraceRecord?,

        adaptations:
            List<DynamicSchedulePriorityAdaptation> =
            emptyList(),

        generatedAt:
            Instant,

        engineVersion:
            String,
    ): DynamicScheduleDecisionProvenance =
        build(
            DynamicScheduleDecisionProvenanceInput(
                snapshot = snapshot,
                trace = trace,
                adaptations = adaptations,
                generatedAt = generatedAt,
                engineVersion = engineVersion,
            ),
        )
}
