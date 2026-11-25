package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

class DynamicScheduleAdaptationBoundaryService(
    private val adapter:
        DynamicScheduleAdaptationBoundaryAdapter =
        DeterministicDynamicScheduleAdaptationBoundaryAdapter(),
) {

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
    ): DynamicScheduleBoundedAdaptation {

        val boundary =
            adapter.apply(
                adaptation =
                    adaptation,

                confidence =
                    confidence,

                state =
                    state,

                now =
                    now,

                policy =
                    policy,
            )

        return DynamicScheduleBoundedAdaptation
            .from(boundary)
    }
}
