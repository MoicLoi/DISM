package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

data class DynamicScheduleContextBindingResult(
    /**
     * Model before context projection.
     */
    val inputModel: DynamicScheduleModel,

    /**
     * Model after context projection.
     */
    val projectedModel: DynamicScheduleModel,

    /**
     * Context evaluation for every schedule item.
     */
    val matches: List<DynamicScheduleContextMatch>,

    /**
     * Actual state changes caused by context projection.
     */
    val changes: List<DynamicScheduleContextBindingChange> = emptyList(),

    /**
     * Non-fatal diagnostics.
     */
    val warnings: List<String> = emptyList(),
) {
    val changed: Boolean
        get() = changes.isNotEmpty()

    val changedCount: Int
        get() = changes.size
}
