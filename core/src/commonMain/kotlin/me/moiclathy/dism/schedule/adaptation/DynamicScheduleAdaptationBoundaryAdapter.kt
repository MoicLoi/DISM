package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

interface DynamicScheduleAdaptationBoundaryAdapter {

    fun apply(
        adaptation:
            DynamicSchedulePriorityAdaptation,

        confidence:
            DynamicScheduleAdaptationConfidence,

        state:
            DynamicScheduleAdaptationBoundaryState,

        now: Instant,

        policy:
            DynamicScheduleAdaptationBoundaryPolicy =
            DynamicScheduleAdaptationBoundaryPolicy(),
    ): DynamicScheduleAdaptationBoundary
}
