package me.moiclathy.dism.event.constraint

import kotlin.time.Instant

/**
 * Time Constraint Engine。
 */
interface TimeConstraintEngine {

    /**
     * 验证一个具体时间是否满足 Constraint。
     */
    fun evaluate(
        constraint: TimeConstraint,
        actualStart: Instant?,
        actualEnd: Instant?,
        evaluatedAt: Instant,
    ): TimeConstraintEvaluation

    /**
     * 验证多个 Constraint。
     */
    fun evaluateAll(
        constraints: List<TimeConstraint>,
        actualStart: Instant?,
        actualEnd: Instant?,
        evaluatedAt: Instant,
    ): TimeConstraintEvaluation
}
