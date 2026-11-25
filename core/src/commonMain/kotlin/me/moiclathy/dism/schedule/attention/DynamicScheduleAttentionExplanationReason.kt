package me.moiclathy.dism.schedule.attention

data class DynamicScheduleAttentionExplanationReason(
    val type: DynamicScheduleAttentionExplanationReasonType,
    val message: String,
    val strength: Double,
    val decisive: Boolean = false,
) {
    init {
        require(message.isNotBlank())
        require(strength in 0.0..1.0)
    }
}
