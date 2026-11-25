package me.moiclathy.dism.attention.explanation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttentionExplanationEngineTest {
    private val engine = AttentionExplanationEngine()

    @Test
    fun explainsAdaptiveBoost() {
        val result = engine.explain(
            selected = true,
            basePriority = 0.50,
            effectivePriority = 0.72,
            adaptiveDelta = 0.22,
        )

        assertTrue(AttentionDecisionReason.ADAPTIVE_BOOST in result.reasons)
        assertTrue(result.summary.contains("历史反馈"))
    }

    @Test
    fun explainsContextSuppression() {
        val result = engine.explain(
            selected = false,
            basePriority = 0.80,
            effectivePriority = 0.80,
            contextSuppressed = true,
        )

        assertTrue(AttentionDecisionReason.CONTEXT_SUPPRESSED in result.reasons)
        assertTrue(result.summary.contains("上下文"))
    }

    @Test
    fun explainsLowPriority() {
        val result = engine.explain(
            selected = false,
            basePriority = 0.10,
            effectivePriority = 0.10,
        )

        assertTrue(AttentionDecisionReason.BELOW_MINIMUM_PRIORITY in result.reasons)
        assertEquals(0.10, result.effectivePriority)
    }

    @Test
    fun explainsCriticalPriority() {
        val result = engine.explain(
            selected = true,
            basePriority = 0.95,
            effectivePriority = 0.95,
        )

        assertTrue(AttentionDecisionReason.CRITICAL_PRIORITY in result.reasons)
    }

    @Test
    fun priorityValuesAreBounded() {
        val result = engine.explain(
            selected = true,
            basePriority = 2.0,
            effectivePriority = -1.0,
        )

        assertEquals(1.0, result.basePriority)
        assertEquals(0.0, result.effectivePriority)
    }
}
