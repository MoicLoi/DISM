package me.moiclathy.dism.schedule.state

import kotlinx.datetime.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleIntelligencePolicy
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/** Input for one deterministic schedule-state projection pass. */
data class DynamicScheduleStateProjectionInput(
    val model: DynamicScheduleModel,
    val evaluatedAt: Instant,
    val policy: DynamicScheduleStateProjectionPolicy = DynamicScheduleStateProjectionPolicy(),
    val intelligencePolicy: DynamicScheduleIntelligencePolicy = DynamicScheduleIntelligencePolicy(),
) {
    init {
        require(model.id.value.isNotBlank())
    }
}
