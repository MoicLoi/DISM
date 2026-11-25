package me.moiclathy.dism.event.temporal

import kotlin.time.Instant
import me.moiclathy.dism.event.model.EventTemporal

/**
 * 确定性的 Temporal State Engine。
 *
 * 只根据：
 *
 * EventTemporal
 * evaluatedAt
 * policy
 *
 * 计算 TemporalState。
 *
 * 不修改 Event。
 * 不修改 State。
 * 不访问数据库。
 * 不触发 Attention。
 * 不执行 Replanning。
 */
class DeterministicTemporalStateEngine(
    private val policy: TemporalStatePolicy =
        TemporalStatePolicy(),
) : TemporalStateEngine {

    override fun evaluate(
        temporal: EventTemporal,
        evaluatedAt: Instant,
    ): TemporalStateEvaluation {

        val state =
            when (temporal) {

                /*
                 * 没有时间信息。
                 */
                EventTemporal.Unscheduled ->
                    TemporalState.UNSCHEDULED

                /*
                 * 单一时间点。
                 *
                 * 例如：
                 *
                 * “14:00 开会”
                 */
                is EventTemporal.At -> {

                    if (
                        evaluatedAt <
                        temporal.start
                    ) {
                        TemporalState.UPCOMING
                    } else {
                        TemporalState.ENDED
                    }
                }

                /*
                 * 时间区间。
                 *
                 * 例如：
                 *
                 * 10:00 - 11:00
                 */
                is EventTemporal.Interval -> {

                    when {

                        evaluatedAt <
                            temporal.start ->
                            TemporalState.UPCOMING

                        evaluatedAt <
                            temporal.end ->
                            TemporalState.IN_PROGRESS

                        else ->
                            TemporalState.ENDED
                    }
                }

                /*
                 * Deadline。
                 */
                is EventTemporal.Deadline -> {

                    when {

                        evaluatedAt <
                            temporal.deadline ->
                            TemporalState.UPCOMING

                        evaluatedAt ==
                            temporal.deadline &&
                            policy.deadlineAtIsDue ->
                            TemporalState.DUE

                        else ->
                            TemporalState.OVERDUE
                    }
                }
            }

        return TemporalStateEvaluation(
            state =
                state,

            evaluatedAt =
                evaluatedAt,
        )
    }
}
