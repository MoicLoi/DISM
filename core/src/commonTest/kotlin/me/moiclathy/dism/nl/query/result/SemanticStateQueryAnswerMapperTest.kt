package me.moiclathy.dism.nl.query.result

import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticStateQueryAnswerMapperTest {
    @Test
    fun packageContainsStableAnswerStatuses() {
        assertEquals(4, SemanticStateQueryAnswerStatus.entries.size)
    }
}
