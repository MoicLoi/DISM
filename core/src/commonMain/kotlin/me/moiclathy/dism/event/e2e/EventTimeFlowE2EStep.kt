package me.moiclathy.dism.event.e2e

/**
 * E2E 单阶段执行结果。
 */
data class EventTimeFlowE2EStep(
    val phase: EventTimeFlowE2EPhase,
    val successful: Boolean,
    val message: String,
) {

    init {
        require(message.isNotBlank()) {
            "message must not be blank."
        }
    }
}
