package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

class DynamicScheduleContextBindingService(
    private val binder: DynamicScheduleContextBinder =
        DeterministicDynamicScheduleContextBinder(),
) {

    fun bind(
        model: DynamicScheduleModel,
        bindings: DynamicScheduleContextBindingSet =
            DynamicScheduleContextBindingSet(),
        activeContextKeys: Set<String> = emptySet(),
        policy: DynamicScheduleContextBindingPolicy =
            DynamicScheduleContextBindingPolicy(),
    ): DynamicScheduleContextBindingResult {

        return binder.bind(
            DynamicScheduleContextBindingInput(
                model = model,
                bindings = bindings,
                activeContextKeys = activeContextKeys,
                policy = policy,
            ),
        )
    }
}
