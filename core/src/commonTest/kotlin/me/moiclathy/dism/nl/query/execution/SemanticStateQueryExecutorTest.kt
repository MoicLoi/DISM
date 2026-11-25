package me.moiclathy.dism.nl.query.execution

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import me.moiclathy.dism.nl.query.SemanticStateQuery

class SemanticStateQueryExecutorTest {
    @Test
    fun unresolvedQueryIsNotExecutable() {
        val query = SemanticStateQuery(subjectEntityId = null)
        val result = DeterministicSemanticStateQueryExecutor().prepare(query)
        assertFalse(result.executable)
    }

    @Test
    fun resolvedQueryIsExecutable() {
        val query = SemanticStateQuery(subjectEntityId = "entity-1")
        val result = DeterministicSemanticStateQueryExecutor().prepare(query)
        assertTrue(result.executable)
    }
}
