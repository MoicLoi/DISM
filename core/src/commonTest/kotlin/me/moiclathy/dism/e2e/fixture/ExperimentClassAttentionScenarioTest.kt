package me.moiclathy.dism.e2e.fixture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.e2e.AttentionCandidateStatus
import me.moiclathy.dism.e2e.RequirementRiskStatus
import me.moiclathy.dism.e2e.VerticalRecommendationStatus
import me.moiclathy.dism.e2e.scenario.ScenarioAssertions

class ExperimentClassAttentionScenarioTest {
    @Test
    fun canonical_fixture_runs_through_attention_and_resolution() {
        val result = ExperimentClassAttentionScenario.runReference()

        assertEquals(
            listOf(
                "semantic-observation",
                "attention-selected",
                "user-completes-reminder",
                "resolved-state",
            ),
            ScenarioAssertions.checkpointNames(result),
        )

        val selected = ScenarioAssertions.checkpointAt(result, 1)
        assertEquals(1, selected.risks.size)
        assertEquals(1, selected.recommendations.size)
        assertEquals(1, selected.attentionCandidates.size)
        assertTrue(selected.attentionCandidates.single().message.contains(ExperimentClassAttentionScenario.REQUIREMENT))

        val resolved = result.finalState
        assertEquals(RequirementRiskStatus.RESOLVED, resolved.risks.single().status)
        assertEquals(VerticalRecommendationStatus.RESOLVED, resolved.recommendations.single().status)
        assertEquals(AttentionCandidateStatus.RESOLVED, resolved.attentionCandidates.single().status)
    }

    @Test
    fun canonical_fixture_is_deterministic() {
        val first = ExperimentClassAttentionScenario.runReference()
        val second = ExperimentClassAttentionScenario.runReference()
        assertEquals(first, second)
    }
}
