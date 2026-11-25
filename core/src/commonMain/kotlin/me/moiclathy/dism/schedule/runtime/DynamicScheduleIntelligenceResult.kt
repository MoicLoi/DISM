package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.attention.runtime.AttentionRuntimeResult
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationResult
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionExplanation
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionProjectionResult
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingResult
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyResolutionResult
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleIntelligenceResult(

    val inputModel:
        me.moiclathy.dism.schedule.model.DynamicScheduleModel,

    val dependencyResult:
        DynamicScheduleDependencyResolutionResult,

    val contextResult:
        DynamicScheduleContextBindingResult,

    val priorityAdaptation:
        DynamicSchedulePriorityAdaptationResult,

    val attentionProjection:
        DynamicScheduleAttentionProjectionResult,

    /**
     * 12.12：
     *
     * 每一个实际进入 Attention 的 schedule item
     * 都可以拥有一个 conflict result。
     */
    val attentionConflicts:
        Map<
            DynamicScheduleItemId,
            DynamicScheduleAttentionConflictResult,
            > = emptyMap(),

    /**
     * 12.13：
     *
     * Explanation 是对最终决策的解释，
     * 而不是额外的决策层。
     */
    val attentionExplanations:
        Map<
            DynamicScheduleItemId,
            DynamicScheduleAttentionExplanation,
            > = emptyMap(),

    val attentionRuntime:
        AttentionRuntimeResult,

    val nextState:
        DynamicScheduleIntelligenceRuntimeState,

    val diagnostics:
        List<String> = emptyList(),
) {

    val conflictCount: Int
        get() = attentionConflicts.size

    val explanationCount: Int
        get() = attentionExplanations.size

    val conflictedItemIds:
        Set<DynamicScheduleItemId>
        get() =
            attentionConflicts
                .filterValues { it.conflict != null }
                .keys
}
