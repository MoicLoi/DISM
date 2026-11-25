package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

data class DynamicScheduleDependencyMatch(
    val dependency: DynamicScheduleDependency,
    val sourceState: DynamicScheduleState,
    val satisfied: Boolean,
    val blocking: Boolean,
    val reason: DynamicScheduleDependencyMatchReason,
) {
    init {
        require(dependency.fromItemId != dependency.toItemId)
    }
}

enum class DynamicScheduleDependencyMatchReason {
    COMPLETION_SATISFIED,
    COMPLETION_REQUIRED,
    START_SATISFIED,
    START_REQUIRED,
    PRECEDENCE_SATISFIED,
    PRECEDENCE_REQUIRED,
    ASSOCIATED_IGNORED,
    ASSOCIATED_BLOCKING,
}
