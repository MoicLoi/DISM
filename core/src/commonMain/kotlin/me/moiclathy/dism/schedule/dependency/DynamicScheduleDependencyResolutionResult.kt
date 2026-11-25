package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

data class DynamicScheduleDependencyResolutionResult(
    val inputModel: DynamicScheduleModel,
    val projectedModel: DynamicScheduleModel,
    val matches: List<DynamicScheduleDependencyMatch>,
    val changes: List<DynamicScheduleDependencyChange> = emptyList(),
    val topologicalOrder: List<me.moiclathy.dism.schedule.model.DynamicScheduleItemId> = emptyList(),
    val cycles: List<List<me.moiclathy.dism.schedule.model.DynamicScheduleItemId>> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val changed: Boolean
        get() = changes.isNotEmpty()

    val blockedCount: Int
        get() = projectedModel.items.count {
            it.state == me.moiclathy.dism.schedule.model.DynamicScheduleState.BLOCKED
        }

    val hasCycles: Boolean
        get() = cycles.isNotEmpty()
}
