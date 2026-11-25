package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

data class EventTimeRuntimePersistenceEvent(
    val id: String,
    val runtimeState: EventTimeRuntimeState,
    val evaluatedAt: Instant,
    val processedEventCount: Int,
    val generatedAlarmCount: Int,
    val metadata: Map<String, String> = emptyMap(),
) {

    init {
        require(id.isNotBlank()) {
            "id must not be blank."
        }

        require(processedEventCount >= 0) {
            "processedEventCount must not be negative."
        }

        require(generatedAlarmCount >= 0) {
            "generatedAlarmCount must not be negative."
        }

        require(generatedAlarmCount <= processedEventCount) {
            "generatedAlarmCount must not exceed processedEventCount."
        }
    }

    companion object {

        fun fromEvaluation(
            id: String,
            evaluation: EventTimeRuntimeEvaluation,
            metadata: Map<String, String> = emptyMap(),
        ): EventTimeRuntimePersistenceEvent {

            return EventTimeRuntimePersistenceEvent(
                id =
                    id,

                runtimeState =
                    evaluation.currentState,

                evaluatedAt =
                    evaluation.evaluatedAt,

                processedEventCount =
                    evaluation.record.processedEventCount,

                generatedAlarmCount =
                    evaluation.record.generatedAlarmCount,

                metadata =
                    metadata,
            )
        }
    }
}
