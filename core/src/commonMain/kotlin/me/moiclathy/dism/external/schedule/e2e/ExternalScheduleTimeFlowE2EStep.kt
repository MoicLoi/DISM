package me.moiclathy.dism.external.schedule.e2e

data class ExternalScheduleTimeFlowE2EStep(
    val phase: ExternalScheduleTimeFlowE2EPhase,
    val successful: Boolean,
    val message: String,
) {

    init {
        require(message.isNotBlank()) {
            "message must not be blank"
        }
    }
}
