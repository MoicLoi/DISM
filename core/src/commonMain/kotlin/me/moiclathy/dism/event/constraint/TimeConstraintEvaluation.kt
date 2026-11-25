package me.moiclathy.dism.event.constraint

import kotlin.time.Instant

/**
 * Time Constraint 综合评估结果。
 */
data class TimeConstraintEvaluation(
    val evaluatedAt: Instant,
    val satisfied: Boolean,
    val violations: List<TimeConstraintViolation> = emptyList(),
) {

    init {
        require(
            satisfied || violations.isNotEmpty()
        ) {
            "An unsatisfied evaluation must contain violations."
        }
    }
}
