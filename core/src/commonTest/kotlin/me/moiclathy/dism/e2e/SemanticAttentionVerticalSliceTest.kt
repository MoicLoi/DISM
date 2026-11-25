package me.moiclathy.dism.e2e

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SemanticAttentionVerticalSliceTest {
    @Test
    fun requirementObservationCreatesRiskRecommendationAndAttention() {
        val slice = SemanticAttentionVerticalSlice()
        val result = slice.observeAll(
            listOf(
                SemanticObservation("o1", "tomorrow-lab", "REQUIRES", "实验服"),
                SemanticObservation("o2", "实验服", "LOCATED_AT", "宿舍"),
            ),
        )

        assertEquals(1, result.risks.size)
        assertEquals(1, result.recommendations.size)
        assertEquals(1, result.attentionCandidates.size)
        assertTrue(result.recommendations.single().message.contains("实验服"))
    }

    @Test
    fun completedFeedbackResolvesEntireVerticalSlice() {
        val slice = SemanticAttentionVerticalSlice()
        val initial = slice.observeAll(
            listOf(
                SemanticObservation("o1", "tomorrow-lab", "REQUIRES", "实验服"),
                SemanticObservation("o2", "实验服", "LOCATED_AT", "宿舍"),
            ),
        )
        val candidate = initial.attentionCandidates.single()
        val recommendation = initial.recommendations.single()

        val result = slice.applyFeedback(
            AttentionFeedback(candidate.key, recommendation.key, AttentionFeedbackType.COMPLETED),
        )

        assertEquals(RequirementRiskStatus.RESOLVED, result.risks.single().status)
        assertEquals(VerticalRecommendationStatus.RESOLVED, result.recommendations.single().status)
        assertEquals(AttentionCandidateStatus.RESOLVED, result.attentionCandidates.single().status)
    }

    @Test
    fun feedbackWithUnknownKeyIsNoOp() {
        val slice = SemanticAttentionVerticalSlice()
        val result = slice.applyFeedback(AttentionFeedback("missing", "missing", AttentionFeedbackType.COMPLETED))
        assertNotNull(result)
        assertTrue(result.risks.isEmpty())
    }

    @Test
    fun stableKeysSurviveRepeatedObservation() {
        val slice = SemanticAttentionVerticalSlice()
        val first = slice.observe(SemanticObservation("o1", "lab", "REQUIRES", "实验服"))
        val firstKey = first.recommendations.single().key
        val second = slice.observe(SemanticObservation("o1", "lab", "REQUIRES", "实验服"))
        assertEquals(firstKey, second.recommendations.single().key)
        assertEquals(1, second.recommendations.size)
    }
}
