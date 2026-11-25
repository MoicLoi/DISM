package me.moiclathy.dism.attention.persistence.sqldelight

import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation
import me.moiclathy.dism.attention.explanation.AttentionDecisionReason
import me.moiclathy.dism.attention.persistence.AttentionEventType

/**
 * Small dependency-free codec used by the SQLDelight adapter.
 *
 * Metadata uses a length-prefixed representation rather than JSON so stage 7.13 does not add
 * a serialization dependency to the core module. The format is deterministic and supports
 * arbitrary String content, including separators and newlines.
 */
object AttentionEventStorageCodec {
    fun encodeMetadata(metadata: Map<String, String>): String = buildString {
        metadata.toList()
            .sortedBy { it.first }
            .forEach { (key, value) ->
                append(key.length)
                append(':')
                append(key)
                append(value.length)
                append(':')
                append(value)
            }
    }

    fun decodeMetadata(payload: String): Map<String, String> {
        if (payload.isEmpty()) return emptyMap()

        val result = linkedMapOf<String, String>()
        var cursor = 0
        while (cursor < payload.length) {
            val keyLength = readLength(payload, cursor)
            cursor = keyLength.nextIndex
            require(cursor + keyLength.length <= payload.length) {
                "Malformed attention metadata: key exceeds payload"
            }
            val key = payload.substring(cursor, cursor + keyLength.length)
            cursor += keyLength.length

            val valueLength = readLength(payload, cursor)
            cursor = valueLength.nextIndex
            require(cursor + valueLength.length <= payload.length) {
                "Malformed attention metadata: value exceeds payload"
            }
            val value = payload.substring(cursor, cursor + valueLength.length)
            cursor += valueLength.length
            result[key] = value
        }
        return result
    }

    fun encodeReasons(explanation: AttentionDecisionExplanation?): String? =
        explanation?.reasons?.joinToString(",") { it.name }

    fun decodeReasons(payload: String?): List<AttentionDecisionReason> {
        if (payload.isNullOrBlank()) return emptyList()
        return payload.split(',').mapNotNull { storedName ->
            AttentionDecisionReason.entries.firstOrNull { it.name == storedName }
        }
    }

    fun decodeEventType(storedName: String): AttentionEventType =
        AttentionEventType.entries.firstOrNull { it.name == storedName }
            ?: error("Unsupported persisted AttentionEventType: $storedName")

    fun decodeFeedbackAction(storedName: String?): AttentionFeedbackAction? {
        if (storedName == null) return null
        return AttentionFeedbackAction.entries.firstOrNull { it.name == storedName }
            ?: error("Unsupported persisted AttentionFeedbackAction: $storedName")
    }

    private data class LengthToken(
        val length: Int,
        val nextIndex: Int,
    )

    private fun readLength(payload: String, startIndex: Int): LengthToken {
        require(startIndex < payload.length) { "Malformed attention metadata: missing length" }
        val separator = payload.indexOf(':', startIndex)
        require(separator >= 0) { "Malformed attention metadata: missing ':'" }
        val length = payload.substring(startIndex, separator).toIntOrNull()
        require(length != null && length >= 0) { "Malformed attention metadata: invalid length" }
        return LengthToken(length = length, nextIndex = separator + 1)
    }
}
