package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingSet
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencySet
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleIntelligenceInput(
    val model: DynamicScheduleModel,

    val dependencies:
        DynamicScheduleDependencySet =
        DynamicScheduleDependencySet(),

    val contextBindings:
        DynamicScheduleContextBindingSet =
        DynamicScheduleContextBindingSet(),

    val activeContextKeys: Set<String> = emptySet(),
)
