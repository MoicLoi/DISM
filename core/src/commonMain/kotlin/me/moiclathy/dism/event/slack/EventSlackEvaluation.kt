package me.moiclathy.dism.event.slack

import kotlin.time.Instant

/**
 * Slack 综合评估结果。
 */
data class EventSlackEvaluation(
    val slack: EventSlack?,
    val status: EventSlackStatus,
    val evaluatedAt: Instant,
    val constraintDescription: String? = null,
) {

    init {
        if (status == EventSlackStatus.UNKNOWN) {
            require(slack == null) {
                "UNKNOWN status must not contain slack."
            }
        } else {
            require(slack != null) {
                "Known Slack status requires slack."
            }
        }

        if (constraintDescription != null) {
            require(constraintDescription.isNotBlank()) {
                "constraintDescription must not be blank."
            }
        }
    }
}
