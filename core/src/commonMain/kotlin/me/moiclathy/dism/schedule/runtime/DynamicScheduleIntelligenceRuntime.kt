package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

interface DynamicScheduleIntelligenceRuntime {

    fun evaluate(
        input: DynamicScheduleIntelligenceInput,
        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),
        now: Instant,
        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),
    ): DynamicScheduleIntelligenceResult

    fun applyFeedback(
        state: DynamicScheduleIntelligenceRuntimeState,
        feedback: DynamicScheduleAttentionFeedback,
    ): DynamicScheduleIntelligenceRuntimeState
}
