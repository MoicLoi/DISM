package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingResult
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * Input for schedule-to-attention projection.
 *
 * contextResult is optional so that the projection layer can also
 * operate on a plain DynamicScheduleModel during early bootstrapping.
 */
data class DynamicScheduleAttentionInput(
    val model: DynamicScheduleModel,
    val contextResult: DynamicScheduleContextBindingResult? = null,
)
