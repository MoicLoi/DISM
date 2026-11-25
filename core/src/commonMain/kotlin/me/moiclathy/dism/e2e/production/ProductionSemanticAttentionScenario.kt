package me.moiclathy.dism.e2e.production

import kotlin.time.Instant
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.attention.context.AttentionContext
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.attention.runtime.AttentionRuntimeState
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.e2e.adapter.SemanticAttentionEngineAdapter
import me.moiclathy.dism.e2e.adapter.SemanticAttentionEvaluation
import me.moiclathy.dism.e2e.fixture.ExperimentClassAttentionScenario
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import kotlin.uuid.Uuid

/**
 * Production wiring seam for the canonical experiment-class scenario.
 *
 * The fixture supplies stable, human-readable observations. The caller owns the conversion
 * from those observations into the project's real SemanticObservationDraft. Once converted,
 * this executor calls the real SemanticAttentionEngineAdapter; it does not recreate semantic
 * or attention logic.
 */
class ProductionSemanticAttentionScenario(
    private val adapter: SemanticAttentionEngineAdapter,
    private val draftFactory: SemanticObservationDraftFactory,
    private val semanticContext: Context,
    private val userId: Uuid,
    private val now: Instant,
    private val attentionContext: AttentionContext = AttentionContext(),
    private val attentionPolicy: AttentionPolicy = AttentionPolicy(),
    private val eventLog: AttentionEventLog? = null,
    private val initialAttentionState: AttentionRuntimeState = AttentionRuntimeState(),
) {

    /** Runs the canonical fixture through the real production adapter. */
    fun evaluateFixture(): SemanticAttentionEvaluation {
        return adapter.evaluate(
            draft = draftFactory.create(ExperimentClassAttentionScenario.observations()),
            semanticContext = semanticContext,
            attentionContext = attentionContext,
            userId = userId,
            now = now,
            previousAttentionState = initialAttentionState,
            eventLog = eventLog,
            attentionPolicy = attentionPolicy,
        )
    }


}

/** Converts stable fixture observations into the project's real semantic observation draft. */
fun interface SemanticObservationDraftFactory {
    fun create(observations: List<me.moiclathy.dism.e2e.SemanticObservation>): SemanticObservationDraft
}
