package me.moiclathy.dism.context.persistence.sqldelight

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.runtime.ContextRuntimeEvent
import me.moiclathy.dism.context.runtime.ContextRuntimeEventLog
import me.moiclathy.dism.context.runtime.ContextRuntimeEventType
import me.moiclathy.dism.persistence.db.DismDatabase

/**
 * SQLDelight Context Runtime Event Log。
 *
 * 将 Context Runtime Event 持久化到 SQLite。
 */
class SqlDelightContextRuntimeEventLog(
    private val database: DismDatabase,
) : ContextRuntimeEventLog {

    private val queries =
        database.contextRuntimeEventQueries

    override fun append(
        event: ContextRuntimeEvent,
    ) {
        queries.insertEvent(
            id = event.id,
            event_type =
                event.type.name,
            occurred_at =
                event.occurredAt.toString(),
            mode =
                event.mode?.name,
            previous_mode =
                event.previousMode?.name,
            confidence =
                event.confidence,
            metadata_payload =
                ContextRuntimeEventStorageCodec
                    .encodeMetadata(
                        event.metadata
                    ),
        )
    }

    override fun appendAll(
        events: Iterable<ContextRuntimeEvent>,
    ) {
        database.transaction {

            events.forEach {
                append(it)
            }
        }
    }

    override fun eventsFor(
        mode: ContextMode,
    ): List<ContextRuntimeEvent> {

        return queries
            .selectForMode(
                mode = mode.name,
            )
            .executeAsList()
            .map {
                toDomain(it)
            }
    }

    override fun all(): List<ContextRuntimeEvent> {

        return queries
            .selectAllEvents()
            .executeAsList()
            .map {
                toDomain(it)
            }
    }

    private fun toDomain(
        row: me.moiclathy.dism.persistence.db.Context_runtime_event,
    ): ContextRuntimeEvent {

        return ContextRuntimeEvent(
            id = row.id,

            type =
                ContextRuntimeEventType
                    .valueOf(
                        row.event_type
                    ),

            occurredAt =
                Instant.parse(
                    row.occurred_at
                ),

            mode =
                row.mode?.let {
                    ContextMode.valueOf(it)
                },

            previousMode =
                row.previous_mode?.let {
                    ContextMode.valueOf(it)
                },

            confidence =
                row.confidence,

            metadata =
                ContextRuntimeEventStorageCodec
                    .decodeMetadata(
                        row.metadata_payload
                    ),
        )
    }
}
