package me.moiclathy.dism.schedule.state

import kotlinx.datetime.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleIntelligencePolicy
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/** Small application boundary around the pure projector. */
class DynamicScheduleStateProjectionService(
    private val projector: DynamicScheduleStateProjector = DeterministicDynamicScheduleStateProjector(),
) {
    fun project(
        model: DynamicScheduleModel,
        evaluatedAt: Instant,
        policy: DynamicScheduleStateProjectionPolicy = DynamicScheduleStateProjectionPolicy(),
        intelligencePolicy: DynamicScheduleIntelligencePolicy = DynamicScheduleIntelligencePolicy(),
    ): DynamicScheduleStateProjectionResult =
        projector.project(
            DynamicScheduleStateProjectionInput(
                model = model,
                evaluatedAt = evaluatedAt,
                policy = policy,
                intelligencePolicy = intelligencePolicy,
            ),
        )
}
