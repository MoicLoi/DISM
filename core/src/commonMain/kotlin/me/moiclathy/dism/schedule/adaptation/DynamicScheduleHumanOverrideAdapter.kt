package me.moiclathy.dism.schedule.adaptation

interface DynamicScheduleHumanOverrideAdapter {

    fun apply(
        input: DynamicScheduleHumanOverrideInput,
        policy: DynamicScheduleHumanOverridePolicy =
            DynamicScheduleHumanOverridePolicy(),
    ): DynamicScheduleHumanOverrideResult
}
