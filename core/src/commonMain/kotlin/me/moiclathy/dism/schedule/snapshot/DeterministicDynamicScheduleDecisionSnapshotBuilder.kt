package me.moiclathy.dism.schedule.snapshot

import kotlin.time.Instant

import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

class DeterministicDynamicScheduleDecisionSnapshotBuilder :
    DynamicScheduleDecisionSnapshotBuilder {

    override fun build(
        item: DynamicScheduleItemSnapshot,

        context: DynamicScheduleContextSnapshot,

        attentionMemory:
            DynamicScheduleAttentionMemorySnapshot,

        dependency:
            DynamicScheduleDependencySnapshot,

        policy:
            DynamicSchedulePolicySnapshot,

        capturedAt: Instant,

        scheduleRevision: Long,

        engineVersion: String,

        traceId: DynamicScheduleDecisionTraceId?,
    ): DynamicScheduleDecisionSnapshot {

        return DynamicScheduleDecisionSnapshot(
            itemId = item.itemId,

            capturedAt = capturedAt,

            scheduleRevision =
                scheduleRevision,

            engineVersion =
                engineVersion,

            item = item,

            context = context,

            attentionMemory =
                attentionMemory,

            dependency =
                dependency,

            policy =
                policy,

            traceId =
                traceId,
        )
    }
}
