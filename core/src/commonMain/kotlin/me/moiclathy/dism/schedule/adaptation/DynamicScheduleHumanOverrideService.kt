package me.moiclathy.dism.schedule.adaptation

interface DynamicScheduleHumanOverrideService {

    fun apply(
        input: DynamicScheduleHumanOverrideInput,
        policy: DynamicScheduleHumanOverridePolicy =
            DynamicScheduleHumanOverridePolicy(),
    ): DynamicScheduleHumanOverrideResult
}
