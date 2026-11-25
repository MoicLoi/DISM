package me.moiclathy.dism.schedule.attention

data class DynamicScheduleAttentionConflictResult(
    val resolvedPriority: Double,

    val conflict:
        DynamicScheduleAttentionConflict?,

    val hardConstraintPresent: Boolean,

    val attentionRequired: Boolean,

    val reasons: List<String> = emptyList(),
)
