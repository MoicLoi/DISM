package me.moiclathy.dism.schedule.replay

data class DynamicScheduleDecisionReplayChangedFactor(
    val name: String,
    val originalValue: String,
    val currentValue: String,
    val impact: String,
) {
    init {
        require(name.isNotBlank())
        require(impact.isNotBlank())
    }
}
