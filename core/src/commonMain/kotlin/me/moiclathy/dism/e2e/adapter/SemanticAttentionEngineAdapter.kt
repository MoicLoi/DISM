package me.moiclathy.dism.e2e.adapter

import kotlin.time.Instant
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.attention.context.AttentionContext
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.attention.runtime.AttentionRuntime
import me.moiclathy.dism.attention.runtime.AttentionRuntimeCandidate
import me.moiclathy.dism.attention.runtime.AttentionRuntimeResult
import me.moiclathy.dism.attention.runtime.AttentionRuntimeState
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.persistence.repository.EntityRepository
import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.semantic.engine.ReactiveEvaluationResult
import me.moiclathy.dism.semantic.engine.SemanticStateEngine
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import kotlin.uuid.Uuid

/**
 * Production-facing adapter between the real SemanticStateEngine and the real AttentionRuntime.
 *
 * SemanticStateEngine remains the source of truth for world state. This adapter only bridges
 * its recommendation projection into AttentionRuntime; it does not duplicate semantic logic.
 */
class SemanticAttentionEngineAdapter(
    private val semanticEngine: SemanticStateEngine,
    private val attentionRuntime: AttentionRuntime = AttentionRuntime(),
) {
    fun evaluate(
        draft: SemanticObservationDraft,
        semanticContext: Context,
        attentionContext: AttentionContext = AttentionContext(),
        userId: Uuid,
        now: Instant,
        previousAttentionState: AttentionRuntimeState = AttentionRuntimeState(),
        eventLog: AttentionEventLog? = null,
        attentionPolicy: AttentionPolicy = AttentionPolicy(),
    ): SemanticAttentionEvaluation {
        val semanticResult = semanticEngine.observeAndEvaluate(
            draft = draft,
            context = semanticContext,
            userId = userId,
            // The semantic engine still needs a policy for its legacy visibleRecommendations
            // projection. The adapter intentionally uses the full recommendation list below.
            attentionPolicy = attentionPolicy,
        )

        val candidates = semanticResult.recommendations
            .mapNotNull { recommendation ->
                recommendation.key?.let { key ->
                    AttentionRuntimeCandidate(
                        recommendation = recommendation,
                        attentionKey = me.moiclathy.dism.attention.lifecycle.AttentionKey(key).toString(),
                    )
                }
            }

        val attentionResult = attentionRuntime.evaluate(
            candidates = candidates,
            context = attentionContext,
            previous = previousAttentionState,
            now = now,
            eventLog = eventLog,
            policy = attentionPolicy,
        )

        return SemanticAttentionEvaluation(
            semantic = semanticResult,
            attention = attentionResult,
        )
    }
}

/** Complete result of one real SemanticStateEngine -> AttentionRuntime cycle. */
data class SemanticAttentionEvaluation(
    val semantic: ReactiveEvaluationResult,
    val attention: AttentionRuntimeResult,
) {
    val recommendations: List<Recommendation>
        get() = semantic.recommendations

    val visibleRecommendations: List<Recommendation>
        get() = attention.selection.selected
}
