package me.moiclathy.dism.schedule.state

import kotlinx.datetime.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/** Result of a pure state-projection pass. */
data class DynamicScheduleStateProjectionResult(
    val inputModel: DynamicScheduleModel,
    val projectedModel: DynamicScheduleModel,
    val evaluatedAt: Instant,
    val changes: List<DynamicScheduleStateProjectionChange> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val changed: Boolean
        get() = changes.isNotEmpty()

    val changedCount: Int
        get() = changes.size

    val unchangedCount: Int
        get() = inputModel.size - changedCount
}
