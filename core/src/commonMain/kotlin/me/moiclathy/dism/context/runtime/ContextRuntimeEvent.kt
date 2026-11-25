package me.moiclathy.dism.context.runtime

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Runtime Event。
 *
 * 用于持久化和恢复。
 */
data class ContextRuntimeEvent(
    val id: String,

    val type: ContextRuntimeEventType,

    val occurredAt: Instant,

    val mode: ContextMode? = null,

    val previousMode: ContextMode? = null,

    val confidence: Double? = null,

    val metadata: Map<String, String> = emptyMap(),
) {

    init {
        require(id.isNotBlank()) {
            "id must not be blank."
        }

        confidence?.let {
            require(it in 0.0..1.0) {
                "confidence must be between 0.0 and 1.0."
            }
        }
    }
}
