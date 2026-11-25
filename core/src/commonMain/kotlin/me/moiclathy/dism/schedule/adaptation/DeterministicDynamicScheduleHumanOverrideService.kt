package me.moiclathy.dism.schedule.adaptation

class DeterministicDynamicScheduleHumanOverrideService(
    private val adapter:
        DynamicScheduleHumanOverrideAdapter =
            DeterministicDynamicScheduleHumanOverrideAdapter(),
) : DynamicScheduleHumanOverrideService {

    override fun apply(
        input: DynamicScheduleHumanOverrideInput,
        policy: DynamicScheduleHumanOverridePolicy,
    ): DynamicScheduleHumanOverrideResult {

        return adapter.apply(
            input = input,
            policy = policy,
        )
    }
}
