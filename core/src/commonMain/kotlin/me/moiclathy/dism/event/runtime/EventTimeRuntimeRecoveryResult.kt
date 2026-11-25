package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * Runtime Recovery 结果。
 */
data class EventTimeRuntimeRecoveryResult(
    val recovered: Boolean,
    val state: EventTimeRuntimeState,
    val restoredAt: Instant,
    val restoredFromEventId: String?,
    val processedEventCount: Int,
    val generatedAlarmCount: Int,
    val historySize: Int,
) {

    init {
        if (recovered) {
            require(restoredFromEventId != null) {
                "A recovered runtime must have a source event."
            }
        }

        if (!recovered) {
            require(restoredFromEventId == null) {
                "A non-recovered runtime must not have a source event."
            }
        }

        require(processedEventCount >= 0) {
            "processedEventCount must not be negative."
        }

        require(generatedAlarmCount >= 0) {
            "generatedAlarmCount must not be negative."
        }

        require(historySize >= 0) {
            "historySize must not be negative."
        }
    }
}
