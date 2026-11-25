package me.moiclathy.dism.schedule.attention

data class DynamicScheduleAttentionConstraint(
    val type: DynamicScheduleAttentionConstraintType,

    /**
     * 0.0..1.0
     */
    val strength: Double,

    val reason: String,
) {
    init {
        require(strength in 0.0..1.0)
        require(reason.isNotBlank())
    }

    val isHard: Boolean
        get() = when (type) {
            DynamicScheduleAttentionConstraintType.EXPLICIT_USER_REQUEST,
            DynamicScheduleAttentionConstraintType.REQUIRED_DEPENDENCY,
            DynamicScheduleAttentionConstraintType.REQUIRED_CONTEXT,
            DynamicScheduleAttentionConstraintType.HARD_TEMPORAL_ANCHOR -> true

            DynamicScheduleAttentionConstraintType.NORMAL_PRIORITY,
            DynamicScheduleAttentionConstraintType.LEARNED_PREFERENCE -> false
        }
}
