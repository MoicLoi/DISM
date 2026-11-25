package me.moiclathy.dism.event.runtime

import kotlin.time.Instant
import me.moiclathy.dism.persistence.db.DismDatabase

/**
 * SQLDelight Runtime Persistence。
 *
 * 复用现有 DismDatabase。
 */
class SqlDelightEventTimeRuntimePersistence(
    database: DismDatabase,
) : EventTimeRuntimePersistence {

    private val queries =
        database.eventTimeRuntimeEventQueries

    override fun append(
        event: EventTimeRuntimePersistenceEvent,
    ) {

        queries.insertEvent(
            id =
                event.id,

            runtime_state =
                event.runtimeState.name,

            evaluated_at =
                event.evaluatedAt.toString(),

            processed_event_count =
                event.processedEventCount.toLong(),

            generated_alarm_count =
                event.generatedAlarmCount.toLong(),

            metadata_payload =
                encodeMetadata(event.metadata),
        )
    }

    override fun all():
        List<EventTimeRuntimePersistenceEvent> {

        return queries
            .selectAll()
            .executeAsList()
            .map(::mapRow)
    }

    override fun latest():
        EventTimeRuntimePersistenceEvent? {

        return queries
            .selectLatest()
            .executeAsOneOrNull()
            ?.let(::mapRow)
    }

    override fun clear() {
        queries.deleteAll()
    }

    private fun mapRow(
        row:
            me.moiclathy.dism.persistence.db.Event_time_runtime_event,
    ): EventTimeRuntimePersistenceEvent {

        return EventTimeRuntimePersistenceEvent(
            id =
                row.id,

            runtimeState =
                EventTimeRuntimeState.valueOf(
                    row.runtime_state
                ),

            evaluatedAt =
                Instant.parse(
                    row.evaluated_at
                ),

            processedEventCount =
                row.processed_event_count.toInt(),

            generatedAlarmCount =
                row.generated_alarm_count.toInt(),

            metadata =
                decodeMetadata(
                    row.metadata_payload
                ),
        )
    }

    private fun encodeMetadata(
        metadata:
            Map<String, String>,
    ): String {

        return metadata
            .entries
            .sortedBy { entry -> entry.key }
            .joinToString("\n") { entry ->

                "${encode(entry.key)}=${encode(entry.value)}"
            }
    }

    private fun decodeMetadata(
        payload:
            String,
    ): Map<String, String> {

        if (payload.isBlank()) {
            return emptyMap()
        }

        return payload
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->

                val separator =
                    line.indexOf('=')

                if (separator <= 0) {
                    null
                } else {

                    val key =
                        decode(
                            line.substring(
                                0,
                                separator,
                            )
                        )

                    val value =
                        decode(
                            line.substring(
                                separator + 1,
                            )
                        )

                    key to value
                }
            }
            .toMap()
    }

    private fun encode(
        value: String,
    ): String {

        return value
            .replace("%", "%25")
            .replace("\n", "%0A")
            .replace("=", "%3D")
    }

    private fun decode(
        value: String,
    ): String {

        return value
            .replace("%3D", "=")
            .replace("%0A", "\n")
            .replace("%25", "%")
    }
}
