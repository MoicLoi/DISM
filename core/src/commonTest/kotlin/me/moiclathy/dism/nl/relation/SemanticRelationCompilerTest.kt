package me.moiclathy.dism.nl.relation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

class SemanticRelationCompilerTest {
    private val subject = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val objectId = Uuid.parse("00000000-0000-0000-0000-000000000002")

    @Test
    fun canonicalizesPredicateAndClampsConfidence() {
        val result = DeterministicSemanticRelationCompiler().compile(subject, " located_at ", objectId, 2.0)
        assertEquals("LOCATED_AT", result.predicate)
        assertEquals(1.0, result.confidence)
    }

    @Test
    fun rejectsSelfRelation() {
        assertFailsWith<IllegalArgumentException> {
            DeterministicSemanticRelationCompiler().compile(subject, "LOCATED_AT", subject, 1.0)
        }
    }

    @Test
    fun batchDeduplicatesAndKeepsHighestConfidence() {
        val compiler = SemanticRelationBatchCompiler()
        val result = compiler.compileAll(
            listOf(
                SemanticRelationBatchCompiler.Input(subject, "located_at", objectId, 0.4),
                SemanticRelationBatchCompiler.Input(subject, "LOCATED_AT", objectId, 0.9),
            ),
        )
        assertEquals(1, result.size)
        assertEquals(0.9, result.single().confidence)
    }
}
