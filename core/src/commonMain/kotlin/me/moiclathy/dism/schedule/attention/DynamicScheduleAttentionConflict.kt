package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleAttentionConflict(
    val itemId: DynamicScheduleItemId,

    val type: DynamicScheduleAttentionConflictType,

    val originalPriority: Double,

    val resolvedPriority: Double,

    val overriddenBy:
        DynamicScheduleAttentionConstraint? = null,

    val reasons: List<String> = emptyList(),
)
