package me.moiclathy.dism.context.runtime

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 某一个 Context Mode 的 Runtime 记录。
 */
data class ContextRuntimeRecord(
    val mode: ContextMode,

    val firstObservedAt: Instant? = null,

    val lastObservedAt: Instant? = null,

    val enteredAt: Instant? = null,

    val exitedAt: Instant? = null,

    val observationCount: Int = 0,

    val transitionCount: Int = 0,

    val lastConfidence: Double = 0.0,
) {

    init {
        require(
            lastConfidence in 0.0..1.0
        ) {
            "lastConfidence must be between 0.0 and 1.0."
        }

        require(observationCount >= 0) {
            "observationCount must not be negative."
        }

        require(transitionCount >= 0) {
            "transitionCount must not be negative."
        }
    }
}
