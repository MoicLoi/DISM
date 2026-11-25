package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

data class DynamicScheduleDependencyChange(
    val itemId: DynamicScheduleItemId,
    val previousState: DynamicScheduleState,
    val projectedState: DynamicScheduleState,
    val reason: DynamicScheduleDependencyChangeReason,
    val unmetDependencies: List<DynamicScheduleDependencyId>,
) {
    init {
        require(previousState != projectedState)
    }
}

enum class DynamicScheduleDependencyChangeReason {
    DEPENDENCY_UNMET,
    DEPENDENCIES_SATISFIED,
}
