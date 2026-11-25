package me.moiclathy.dism.e2e.fixture

import me.moiclathy.dism.e2e.SemanticObservation
import me.moiclathy.dism.e2e.SemanticObservationResult

/**
 * Stable fixture contract for swapping the reference slice with the production
 * SemanticAttentionEngineAdapter without changing scenario data or assertions.
 */
fun interface ExperimentClassScenarioExecutor {
    fun execute(observations: List<SemanticObservation>): SemanticObservationResult
}
