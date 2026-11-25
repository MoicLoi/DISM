package me.moiclathy.dism.schedule.trace

data class DynamicScheduleDecisionTraceEntry(
    val stage: DynamicScheduleDecisionTraceStage,

    val message: String,

    val values: Map<String, String> = emptyMap(),

    val decisive: Boolean = false,
) {
    init {
        require(message.isNotBlank())
    }
}
