package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionDecayPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictPolicy
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionExplanationPolicy
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionPolicy
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingPolicy
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyPolicy
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleIntelligenceRuntimePolicy(

    val dependencyPolicy:
        DynamicScheduleDependencyPolicy =
        DynamicScheduleDependencyPolicy(),

    val contextPolicy:
        DynamicScheduleContextBindingPolicy =
        DynamicScheduleContextBindingPolicy(),

    val decayPolicy:
        DynamicScheduleAttentionDecayPolicy =
        DynamicScheduleAttentionDecayPolicy(),

    val adaptationPolicy:
        DynamicSchedulePriorityAdaptationPolicy =
        DynamicSchedulePriorityAdaptationPolicy(),

    val attentionProjectionPolicy:
        DynamicScheduleAttentionPolicy =
        DynamicScheduleAttentionPolicy(),

    val attentionPolicy:
        AttentionPolicy =
        AttentionPolicy(),

    /**
     * 12.21：
     *
     * Conflict Resolver 是 Schedule → Attention
     * 最终决策之前的硬约束层。
     */
    val conflictPolicy:
        DynamicScheduleAttentionConflictPolicy =
        DynamicScheduleAttentionConflictPolicy(),

    /**
     * 12.21：
     *
     * Explanation 只负责解释已经确定的决策，
     * 不参与决策本身。
     */
    val explanationPolicy:
        DynamicScheduleAttentionExplanationPolicy =
        DynamicScheduleAttentionExplanationPolicy(),
)
