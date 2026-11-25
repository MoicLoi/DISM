package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingResult
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * Application-facing service for projecting schedules into Attention.
 */
class DynamicScheduleAttentionProjectionService(
    private val projector: DynamicScheduleAttentionProjector,
) {

    fun project(
        model: DynamicScheduleModel,
        contextResult: DynamicScheduleContextBindingResult? = null,
        policy: DynamicScheduleAttentionPolicy =
            DynamicScheduleAttentionPolicy(),
    ): DynamicScheduleAttentionProjectionResult {

        return projector.project(
            input =
                DynamicScheduleAttentionInput(
                    model = model,
                    contextResult = contextResult,
                ),
            policy = policy,
        )
    }
}
