package me.moiclathy.dism.schedule.snapshot

interface DynamicScheduleDecisionSnapshotBuilder {

    fun build(
        item: DynamicScheduleItemSnapshot,

        context: DynamicScheduleContextSnapshot,

        attentionMemory:
            DynamicScheduleAttentionMemorySnapshot,

        dependency:
            DynamicScheduleDependencySnapshot,

        policy:
            DynamicSchedulePolicySnapshot,

        capturedAt: kotlin.time.Instant,

        scheduleRevision: Long,

        engineVersion: String,

        traceId:
            me.moiclathy.dism.schedule.trace
                .DynamicScheduleDecisionTraceId? = null,
    ): DynamicScheduleDecisionSnapshot
}
