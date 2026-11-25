package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

class DynamicScheduleDependencyResolutionService(
    private val resolver: DynamicScheduleDependencyResolver =
        DeterministicDynamicScheduleDependencyResolver(),
) {
    fun resolve(
        model: DynamicScheduleModel,
        dependencies: DynamicScheduleDependencySet = DynamicScheduleDependencySet(),
        policy: DynamicScheduleDependencyPolicy = DynamicScheduleDependencyPolicy(),
    ): DynamicScheduleDependencyResolutionResult =
        resolver.resolve(
            DynamicScheduleDependencyInput(
                model = model,
                dependencies = dependencies,
                policy = policy,
            ),
        )
}
