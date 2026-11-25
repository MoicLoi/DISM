package me.moiclathy.dism.attention

import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import me.moiclathy.dism.planning.recommendation.RecommendationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttentionEngineTest {

    private val engine = AttentionEngine()

    @Test
    fun select_shouldKeepHighestPriorityRecommendations() {
        val recommendations = listOf(
            recommendation(0.9),
            recommendation(0.7),
            recommendation(0.5),
            recommendation(0.2),
        )

        val selected = engine.select(
            recommendations = recommendations,
            policy = AttentionPolicy(
                softLimit = 2,
                hardLimit = 4,
                minimumPriority = 0.3,
            ),
        )

        assertEquals(2, selected.size)
        assertEquals(0.9, selected[0].priority)
        assertEquals(0.7, selected[1].priority)
    }

    @Test
    fun selectWithBudget_shouldAllowCriticalOverflowOnlyWhenEnabled() {
        val recommendations = listOf(
            recommendation(0.99),
            recommendation(0.96),
            recommendation(0.91),
            recommendation(0.88),
            recommendation(0.40),
        )

        val result = engine.selectWithBudget(
            recommendations = recommendations,
            policy = AttentionPolicy(
                softLimit = 2,
                hardLimit = 4,
                minimumPriority = 0.3,
                criticalPriority = 0.90,
                allowCriticalOverflow = true,
            ),
        )

        assertEquals(3, result.selected.size)
        assertEquals(0.99, result.selected[0].priority)
        assertEquals(0.96, result.selected[1].priority)
        assertEquals(0.91, result.selected[2].priority)
        assertEquals(2, result.suppressed.size)
        assertTrue(result.softLimitReached)
    }

    @Test
    fun selectWithBudget_shouldNeverExceedHardLimit() {
        val recommendations = listOf(
            recommendation(0.99),
            recommendation(0.98),
            recommendation(0.97),
            recommendation(0.96),
            recommendation(0.95),
            recommendation(0.94),
        )

        val result = engine.selectWithBudget(
            recommendations = recommendations,
            policy = AttentionPolicy(
                softLimit = 2,
                hardLimit = 4,
                minimumPriority = 0.0,
                criticalPriority = 0.90,
                allowCriticalOverflow = true,
            ),
        )

        assertEquals(4, result.selected.size)
        assertTrue(result.hardLimitReached)
        assertEquals(2, result.suppressed.size)
    }

    private fun recommendation(
        priority: Double,
    ) = Recommendation(
        type = RecommendationType.OTHER,
        message = RecommendationMessage.Generic(
            key = "test-$priority",
        ),
        priority = priority,
        confidence = 1.0,
    )
}
