package me.moiclathy.dism.nl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DeterministicSemanticNormalizerTest {
    @Test
    fun trimsAndCollapsesWhitespace() {
        val input = NaturalLanguageInput("  明天   实验课   带实验服  ", observedAt = Instant.parse("2026-09-15T06:00:00Z"))
        assertEquals("明天 实验课 带实验服", DeterministicSemanticNormalizer.normalize(input).text)
    }

    @Test
    fun normalizationDoesNotChangeMetadata() {
        val input = NaturalLanguageInput(" x ", source = "chat", observedAt = Instant.parse("2026-09-15T06:00:00Z"), conversationId = "c1")
        val normalized = DeterministicSemanticNormalizer.normalize(input)
        assertEquals(input.source, normalized.source)
        assertEquals(input.observedAt, normalized.observedAt)
        assertEquals(input.conversationId, normalized.conversationId)
    }
}
