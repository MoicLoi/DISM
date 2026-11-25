package me.moiclathy.dism.semantic.engine

import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.semantic.propagation.PropagationResult
import me.moiclathy.dism.planning.recommendation.RecommendationLifecycleChange

/**
 * observeAndEvaluate() 的结果：
 * 一次状态变化 + 它触发的语义后果。
 *
 * propagationResults 用列表而非单个 nullable：
 * 一个 Observation 未来可能产生多个 Transition，
 * 每个 Transition 都有自己对应的传播结果。
 */
data class ReactiveEvaluationResult(
    val observeResult: ObserveResult,
    val propagationResults: List<PropagationResult>,
    val recommendations: List<Recommendation>,
    val visibleRecommendations: List<Recommendation>,
    val recommendationLifecycleChanges: List<RecommendationLifecycleChange> = emptyList(),
)
