package me.moiclathy.dism.event.e2e

/**
 * Time-flow E2E 最终结果。
 */
data class EventTimeFlowE2EResult(
    val trace: EventTimeFlowE2ETrace,
    val contractSatisfied: Boolean,
    val errors: List<String>,
) {

    init {
        if (contractSatisfied) {
            require(errors.isEmpty()) {
                "A satisfied contract must not contain errors."
            }
        }
    }

    val successful: Boolean
        get() =
            contractSatisfied &&
                trace.successful
}
