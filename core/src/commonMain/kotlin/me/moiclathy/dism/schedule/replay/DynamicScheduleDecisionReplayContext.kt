package me.moiclathy.dism.schedule.replay

data class DynamicScheduleDecisionReplayContext(
    val currentPriority: Double,
    val currentAttentionRequired: Boolean,
    val currentConflict:
        me.moiclathy.dism.schedule.attention
            .DynamicScheduleAttentionConflictResult? = null,
) {
    init {
        require(currentPriority in 0.0..1.0)
    }
}
