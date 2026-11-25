package me.moiclathy.dism.schedule.adaptation.audit

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

data class DynamicScheduleDecisionAudit(
    val itemId:
        DynamicScheduleItemId,

    val snapshotId:
        DynamicScheduleDecisionSnapshotId? = null,

    val traceId:
        DynamicScheduleDecisionTraceId? = null,

    val createdAt:
        Instant,

    val status:
        DynamicScheduleDecisionAuditStatus,

    val events:
        List<DynamicScheduleDecisionAuditEvent>,

    val diagnostics:
        List<String> = emptyList(),
) {

    init {
        require(events.isNotEmpty())

        val ids =
            events.map {
                it.id
            }

        require(
            ids.distinct().size == ids.size,
        )
    }

    val eventCount: Int
        get() =
            events.size

    val latestEvent:
        DynamicScheduleDecisionAuditEvent?
        get() =
            events.maxByOrNull {
                it.occurredAt
            }
}
