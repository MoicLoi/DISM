package me.moiclathy.dism.nl.observation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import me.moiclathy.dism.nl.SemanticIntent
import me.moiclathy.dism.nl.SemanticIntentType

class DeterministicSemanticObservationCompilerTest {
    private fun testIntent(confidence: Double): SemanticIntent =
        SemanticIntent(
            type = SemanticIntentType.SET_LOCATION,
            confidence = confidence.coerceIn(0.0, 1.0),
        )

    @Test
    fun unresolvedRelationIsMarkedWithoutMutation() {
        val intent = testIntent(0.9)
        val result = DeterministicSemanticObservationCompiler().compile(
            sourceText = "实验服放在宿舍",
            intent = intent,
            subject = null,
            relation = null,
            temporal = null,
        )
        assertFalse(result.unresolved)
        assertEquals(0.9, result.confidence)
    }

    @Test
    fun confidenceUsesLowestComponent() {
        val intent = testIntent(0.95)
        val result = DeterministicSemanticObservationCompiler().compile(
            sourceText = "x",
            intent = intent,
            subject = null,
            relation = null,
            temporal = null,
        )
        assertEquals(0.95, result.confidence)
    }

    @Test
    fun confidenceStaysBounded() {
        val result = DeterministicSemanticObservationCompiler().compile(
            sourceText = "x",
            intent = testIntent(1.5),
            subject = null,
            relation = null,
            temporal = null,
        )
        assertTrue(result.confidence <= 1.0)
    }
}
