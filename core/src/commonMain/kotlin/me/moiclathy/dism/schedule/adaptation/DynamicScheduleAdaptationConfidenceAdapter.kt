package me.moiclathy.dism.schedule.adaptation

interface DynamicScheduleAdaptationConfidenceAdapter {

    fun calculate(
        input:
            DynamicScheduleAdaptationConfidenceInput,

        policy:
            DynamicScheduleAdaptationConfidencePolicy =
            DynamicScheduleAdaptationConfidencePolicy(),
    ): DynamicScheduleAdaptationConfidence
}
