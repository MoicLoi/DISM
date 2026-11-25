package me.moiclathy.dism.attention.persistence.sqldelight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation
import me.moiclathy.dism.attention.explanation.AttentionDecisionReason
import me.moiclathy.dism.attention.persistence.AttentionEventType

class AttentionEventStorageCodecTest {
    @Test
    fun metadataRoundTripsArbitraryContent() {
        val metadata = linkedMapOf(
            "source" to "runtime",
            "contains:separator" to "line1\nline2,中文:✓",
            "" to "empty-key-is-still-valid",
        )

        val encoded = AttentionEventStorageCodec.encodeMetadata(metadata)
        val decoded = AttentionEventStorageCodec.decodeMetadata(encoded)

        assertEquals(metadata, decoded)
    }

    @Test
    fun metadataEncodingIsDeterministic() {
        val first = AttentionEventStorageCodec.encodeMetadata(mapOf("b" to "2", "a" to "1"))
        val second = AttentionEventStorageCodec.encodeMetadata(mapOf("a" to "1", "b" to "2"))

        assertEquals(first, second)
    }

    @Test
    fun explanationReasonsRoundTripKnownValues() {
        val explanation = AttentionDecisionExplanation(
            selected = true,
            basePriority = 0.7,
            effectivePriority = 0.9,
            reasons = listOf(
                AttentionDecisionReason.ADAPTIVE_BOOST,
                AttentionDecisionReason.SELECTED_BY_BUDGET,
            ),
            summary = "selected",
        )

        val encoded = AttentionEventStorageCodec.encodeReasons(explanation)

        assertEquals(explanation.reasons, AttentionEventStorageCodec.decodeReasons(encoded))
    }

    @Test
    fun duplicateOrFutureReasonNamesDoNotBreakKnownReasons() {
        assertEquals(
            listOf(AttentionDecisionReason.HIGH_PRIORITY),
            AttentionEventStorageCodec.decodeReasons("FUTURE_REASON,HIGH_PRIORITY"),
        )
    }

    @Test
    fun unknownEventTypeFailsLoudly() {
        assertFailsWith<IllegalStateException> {
            AttentionEventStorageCodec.decodeEventType("FUTURE_EVENT")
        }
        assertEquals(
            AttentionEventType.DECISION,
            AttentionEventStorageCodec.decodeEventType("DECISION"),
        )
    }
}
