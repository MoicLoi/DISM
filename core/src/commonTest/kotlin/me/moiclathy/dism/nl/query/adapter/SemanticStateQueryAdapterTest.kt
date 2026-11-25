package me.moiclathy.dism.nl.query.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.nl.query.SemanticStateQuery

class SemanticStateQueryAdapterTest {
    @Test
    fun executesResolvedQueryWithoutMutatingSource() {
        val source = InMemorySemanticStateQueryStateSource(
            listOf(SemanticStateQueryValue(subjectId = "coat", predicate = "located_at", objectName = "dorm", confidence = 0.95))
        )
        val adapter = SemanticStateQueryAdapter(source)
        val query = SemanticStateQuery(subjectEntityId = "coat", predicate = "located_at")
        val result = adapter.execute(query)
        assertTrue(result.found)
        assertEquals("dorm", result.values.single().objectName)
    }
}
