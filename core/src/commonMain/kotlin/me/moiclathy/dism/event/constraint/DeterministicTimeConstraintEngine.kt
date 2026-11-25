package me.moiclathy.dism.event.constraint

import kotlin.time.Instant

/**
 * 确定性的 Time Constraint Engine。
 *
 * 只负责判断时间是否满足约束。
 *
 * 不负责：
 *
 * - Duration 计算
 * - Slack
 * - Alarm
 * - Replanning
 * - Attention
 */
class DeterministicTimeConstraintEngine(
    private val policy:
        TimeConstraintPolicy =
        TimeConstraintPolicy(),
) : TimeConstraintEngine {

    override fun evaluate(
        constraint: TimeConstraint,
        actualStart: Instant?,
        actualEnd: Instant?,
        evaluatedAt: Instant,
    ): TimeConstraintEvaluation {

        val violation =
            evaluateViolation(
                constraint =
                    constraint,

                actualStart =
                    actualStart,

                actualEnd =
                    actualEnd,
            )

        return TimeConstraintEvaluation(
            evaluatedAt =
                evaluatedAt,

            satisfied =
                violation == null,

            violations =
                if (violation == null) {
                    emptyList()
                } else {
                    listOf(violation)
                },
        )
    }

    override fun evaluateAll(
        constraints: List<TimeConstraint>,
        actualStart: Instant?,
        actualEnd: Instant?,
        evaluatedAt: Instant,
    ): TimeConstraintEvaluation {

        val violations =
            constraints.mapNotNull {
                evaluateViolation(
                    constraint =
                        it,

                    actualStart =
                        actualStart,

                    actualEnd =
                        actualEnd,
                )
            }

        return TimeConstraintEvaluation(
            evaluatedAt =
                evaluatedAt,

            satisfied =
                violations.isEmpty(),

            violations =
                violations,
        )
    }

    private fun evaluateViolation(
        constraint: TimeConstraint,
        actualStart: Instant?,
        actualEnd: Instant?,
    ): TimeConstraintViolation? {

        return when (constraint.type) {

            TimeConstraintType.EARLIEST_START -> {

                val boundary =
                    constraint.start!!

                if (actualStart == null) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Actual start time is missing.",
                    )
                } else if (
                    if (policy.inclusiveBoundaries) {
                        actualStart < boundary
                    } else {
                        actualStart <= boundary
                    }
                ) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Event started too early.",
                    )
                } else {
                    null
                }
            }

            TimeConstraintType.LATEST_START -> {

                val boundary =
                    constraint.start!!

                if (actualStart == null) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Actual start time is missing.",
                    )
                } else if (
                    if (policy.inclusiveBoundaries) {
                        actualStart > boundary
                    } else {
                        actualStart >= boundary
                    }
                ) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Event started too late.",
                    )
                } else {
                    null
                }
            }

            TimeConstraintType.LATEST_END -> {

                val boundary =
                    constraint.end!!

                if (actualEnd == null) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Actual end time is missing.",
                    )
                } else if (
                    if (policy.inclusiveBoundaries) {
                        actualEnd > boundary
                    } else {
                        actualEnd >= boundary
                    }
                ) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Event ended too late.",
                    )
                } else {
                    null
                }
            }

            TimeConstraintType.FIXED_TIME -> {

                val fixed =
                    constraint.start!!

                if (actualStart == null) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Actual start time is missing.",
                    )
                } else if (
                    actualStart != fixed
                ) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Event did not occur at the fixed time.",
                    )
                } else {
                    null
                }
            }

            TimeConstraintType.TIME_WINDOW -> {

                val windowStart =
                    constraint.start!!

                val windowEnd =
                    constraint.end!!

                if (actualStart == null) {
                    TimeConstraintViolation(
                        constraint =
                            constraint,

                        message =
                            "Actual start time is missing.",
                    )
                } else {

                    val valid =
                        if (policy.inclusiveBoundaries) {
                            actualStart >= windowStart &&
                                actualStart <= windowEnd
                        } else {
                            actualStart > windowStart &&
                                actualStart < windowEnd
                        }

                    if (!valid) {
                        TimeConstraintViolation(
                            constraint =
                                constraint,

                            message =
                                "Event occurred outside the allowed time window.",
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }
}
