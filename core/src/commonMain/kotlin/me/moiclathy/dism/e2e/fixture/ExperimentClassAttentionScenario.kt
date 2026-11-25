package me.moiclathy.dism.e2e.fixture

import me.moiclathy.dism.e2e.AttentionFeedback
import me.moiclathy.dism.e2e.AttentionFeedbackType
import me.moiclathy.dism.e2e.SemanticAttentionVerticalSlice
import me.moiclathy.dism.e2e.SemanticObservation
import me.moiclathy.dism.e2e.SemanticObservationResult
import me.moiclathy.dism.e2e.scenario.DeterministicScenarioRunner
import me.moiclathy.dism.e2e.scenario.E2eScenarioResult
import me.moiclathy.dism.e2e.scenario.E2eScenarioStep

/**
 * Canonical regression fixture for the "实验课 -> 实验服 -> Attention" vertical slice.
 *
 * The fixture intentionally uses the existing vertical-slice seam rather than duplicating
 * SemanticStateEngine or AttentionRuntime logic. Production integration can replace the
 * executor while keeping the same scenario inputs and checkpoints.
 */
object ExperimentClassAttentionScenario {
    const val EVENT_KEY: String = "tomorrow-lab"
    const val REQUIREMENT: String = "实验服"
    const val LOCATION: String = "宿舍"

    fun observations(): List<SemanticObservation> = listOf(
        SemanticObservation("obs-event", EVENT_KEY, "REQUIRES", REQUIREMENT, 1.0),
        SemanticObservation("obs-location", REQUIREMENT, "LOCATED_AT", LOCATION, 1.0),
    )

    fun initialSteps(): List<E2eScenarioStep<SemanticObservationResult>> = listOf(
        E2eScenarioStep("observe-experiment-class") { state ->
            state
        },
    )

    /** Runs the canonical reference fixture and records named checkpoints. */
    fun runReference(): E2eScenarioResult<SemanticObservationResult> {
        val slice = SemanticAttentionVerticalSlice()
        val initial = SemanticObservationResult(emptyList(), emptyList(), emptyList(), emptyList())
        val steps = listOf(
            E2eScenarioStep<SemanticObservationResult>("semantic-observation") { slice.observeAll(observations()) },
            E2eScenarioStep<SemanticObservationResult>("attention-selected") { slice.snapshot() },
            E2eScenarioStep<SemanticObservationResult>("user-completes-reminder") {
                val snapshot = slice.snapshot()
                val candidate = snapshot.attentionCandidates.single()
                val recommendation = snapshot.recommendations.single()
                slice.applyFeedback(
                    AttentionFeedback(
                        attentionKey = candidate.key,
                        recommendationKey = recommendation.key,
                        type = AttentionFeedbackType.COMPLETED,
                    ),
                )
            },
            E2eScenarioStep<SemanticObservationResult>("resolved-state") { slice.snapshot() },
        )
        return DeterministicScenarioRunner<SemanticObservationResult>().run(initial, steps)
    }
}
