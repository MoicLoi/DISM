package me.moiclathy.dism.schedule.trace

import kotlin.time.Instant

import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationResult
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionExplanation
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionProjectionResult
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingResult
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyResolutionResult
import me.moiclathy.dism.schedule.model.DynamicScheduleItem

/**
 * 13.2
 *
 * Decision Trace 所需要的事实集合。
 *
 * Trace 层只消费"已经计算完成的 Decision Evidence"，
 * 不应该反向依赖 Runtime。
 *
 * 这是 13.2 要解决的核心问题：
 *
 * ```text
 * 修复前：
 * Runtime ──► Trace ──► Runtime     （循环）
 *
 * 修复后：
 * Runtime ──► Evidence ──► Trace    （单向）
 * ```
 *
 * 与设计稿的两处偏差（以实际签名为准）：
 *
 * 1. `attentionSelectedKeys` / `attentionSuppressedKeys`
 *    是 `Set<RecommendationKey>` 而不是 `Set<String>`：
 *    Attention selection 里的 key 本来就是
 *    [RecommendationKey]，转成 String 再比会丢失类型安全。
 *
 * 2. 增加了 [scheduleRevision]：
 *    Trace 的 OUTPUT 阶段需要记录本次决策对应的 Schedule revision，
 *    而这一信息只能由 Runtime 侧提供。
 *    它是 primitive，不会把 runtime 依赖带回来。
 */
data class DynamicScheduleDecisionTraceEvidence(
    val item:
        DynamicScheduleItem,

    val dependencyResult:
        DynamicScheduleDependencyResolutionResult,

    val contextResult:
        DynamicScheduleContextBindingResult,

    val priorityAdaptation:
        DynamicSchedulePriorityAdaptationResult,

    val attentionProjection:
        DynamicScheduleAttentionProjectionResult,

    /**
     * 本次决策中被 Attention Runtime 选中的 key。
     */
    val attentionSelectedKeys:
        Set<RecommendationKey> = emptySet(),

    /**
     * 本次决策中被 Attention Runtime 抑制的 key。
     */
    val attentionSuppressedKeys:
        Set<RecommendationKey> = emptySet(),

    val conflict:
        DynamicScheduleAttentionConflictResult? = null,

    val explanation:
        DynamicScheduleAttentionExplanation? = null,

    val scheduleRevision: Long = 0L,

    val now:
        Instant,
) {
    init {
        require(scheduleRevision >= 0) {
            "scheduleRevision must be >= 0"
        }
    }
}
