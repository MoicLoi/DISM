package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

data class DynamicScheduleDependencyInput(
    val model: DynamicScheduleModel,
    val dependencies: DynamicScheduleDependencySet = DynamicScheduleDependencySet(),
    val policy: DynamicScheduleDependencyPolicy = DynamicScheduleDependencyPolicy(),
) {
    private val itemIds: Set<me.moiclathy.dism.schedule.model.DynamicScheduleItemId>
        get() = model.items.map { it.id }.toSet()

    init {
        require(dependencies.dependencies.all {
            it.fromItemId in itemIds && it.toItemId in itemIds
        }) {
            "every dependency must reference an item in the schedule model"
        }
    }
}
