package me.moiclathy.dism.attention.persistence.sqldelight

import kotlin.time.Instant
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.persistence.db.AttentionEventQueries
import me.moiclathy.dism.persistence.db.DismDatabase

/**
 * SQLDelight-backed append-only AttentionEventLog.
 *
 * Event IDs are primary keys and INSERT OR IGNORE makes replayed/appended events idempotent.
 * Ordering is deterministic: occurredAt first, then event id.
 */
class SqlDelightAttentionEventLog(
    private val queries: AttentionEventQueries,
) : AttentionEventLog {

    constructor(database: DismDatabase) : this(database.attentionEventQueries)

    override fun append(event: AttentionEvent) {
        insert(event)
    }

    override fun appendAll(events: Iterable<AttentionEvent>) {
        queries.transaction {
            events.forEach(::insert)
        }
    }

    override fun eventsFor(attentionKey: String): List<AttentionEvent> =
        queries.selectForAttentionKey(attentionKey, ::mapRow).executeAsList()

    override fun all(): List<AttentionEvent> =
        queries.selectAllEvents(::mapRow).executeAsList()

    /** Useful for diagnostics without exposing deletion through the append-only interface. */
    fun containsEvent(id: String): Boolean =
        queries.countById(id).executeAsOne() > 0L

    private fun insert(event: AttentionEvent) {
        val explanation = event.explanation
        queries.insertEvent(
            id = event.id,
            attention_key = event.attentionKey,
            event_type = event.type.name,
            occurred_at = event.occurredAt.toString(),
            action = event.action,
            feedback_action = event.feedbackAction?.name,
            explanation_selected = explanation?.selected?.let { if (it) 1L else 0L },
            explanation_base_priority = explanation?.basePriority,
            explanation_effective_priority = explanation?.effectivePriority,
            explanation_reasons = AttentionEventStorageCodec.encodeReasons(explanation),
            explanation_summary = explanation?.summary,
            metadata_payload = AttentionEventStorageCodec.encodeMetadata(event.metadata),
        )
    }

    @Suppress("LongParameterList")
    private fun mapRow(
        id: String,
        attentionKey: String,
        eventType: String,
        occurredAt: String,
        action: String?,
        feedbackAction: String?,
        explanationSelected: Long?,
        explanationBasePriority: Double?,
        explanationEffectivePriority: Double?,
        explanationReasons: String?,
        explanationSummary: String?,
        metadataPayload: String,
    ): AttentionEvent {
        val explanation = decodeExplanation(
            selected = explanationSelected,
            basePriority = explanationBasePriority,
            effectivePriority = explanationEffectivePriority,
            reasons = explanationReasons,
            summary = explanationSummary,
        )

        return AttentionEvent(
            id = id,
            attentionKey = attentionKey,
            type = AttentionEventStorageCodec.decodeEventType(eventType),
            occurredAt = Instant.parse(occurredAt),
            action = action,
            feedbackAction = AttentionEventStorageCodec.decodeFeedbackAction(feedbackAction),
            explanation = explanation,
            metadata = AttentionEventStorageCodec.decodeMetadata(metadataPayload),
        )
    }

    private fun decodeExplanation(
        selected: Long?,
        basePriority: Double?,
        effectivePriority: Double?,
        reasons: String?,
        summary: String?,
    ): AttentionDecisionExplanation? {
        val hasAnyValue = selected != null ||
            basePriority != null ||
            effectivePriority != null ||
            reasons != null ||
            summary != null
        if (!hasAnyValue) return null

        require(selected != null && basePriority != null && effectivePriority != null) {
            "Persisted attention explanation is incomplete"
        }

        return AttentionDecisionExplanation(
            selected = selected != 0L,
            basePriority = basePriority,
            effectivePriority = effectivePriority,
            reasons = AttentionEventStorageCodec.decodeReasons(reasons),
            summary = summary.orEmpty(),
        )
    }
}
