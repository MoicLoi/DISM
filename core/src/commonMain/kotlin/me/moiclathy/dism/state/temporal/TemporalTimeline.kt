package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.query.TemporalStateChange
import kotlin.time.Instant

/**
 * 一个实体在某个语义维度上的时间线只读视图。
 *
 * Timeline 不负责修改 RelationState，也不负责决定冲突规则；
 * 它只负责回答：
 *
 * - 这一时间线有哪些历史段？
 * - 某个时刻哪些状态成立？
 * - 当前哪些状态仍成立？
 * - 某个时刻发生了哪些开始/结束？
 * - 某个时间窗口与哪些历史段相交？
 *
 * 因此 TemporalReconciler 负责“重建”，TemporalTimeline 负责“读取”。
 */
class TemporalTimeline private constructor(
    states: List<RelationState>,
) {

    /** 始终按 validFrom 排序的不可变快照。 */
    val states: List<RelationState> =
        states.sortedWith(
            compareBy<RelationState> { it.validFrom }
                .thenBy { it.validUntil }
        )

    /** 当前仍然有效的状态。 */
    fun current(): List<RelationState> =
        states.filter { it.isActive }

    /** 某一时刻成立的状态，采用 [validFrom, validUntil) 语义。 */
    fun at(instant: Instant): List<RelationState> =
        states.filter { it.isValidAt(instant) }

    /**
     * 返回与 [start, end) 有时间交集的历史段。
     *
     * 例如状态 10:00–11:00 与窗口 10:30–12:00 相交，因此会返回。
     */
    fun intersecting(
        start: Instant,
        end: Instant,
    ): List<RelationState> {
        require(end >= start) {
            "Timeline query end cannot be earlier than start"
        }

        return states.filter { state ->
            val stateEnd = state.validUntil

            val startsBeforeWindowEnds =
                state.validFrom < end

            val endsAfterWindowStarts =
                stateEnd == null || stateEnd > start

            startsBeforeWindowEnds && endsAfterWindowStarts
        }
    }

    /**
     * 某一时刻发生的状态变化。
     *
     * Ended 会出现在 validUntil == instant；
     * Started 会出现在 validFrom == instant。
     */
    fun changesAt(
        instant: Instant,
    ): List<TemporalStateChange> =
        buildList {
            states.forEach { state ->
                if (state.validUntil == instant) {
                    add(TemporalStateChange.Ended(state))
                }
                if (state.validFrom == instant) {
                    add(TemporalStateChange.Started(state))
                }
            }
        }

    /** 时间线中最后一个已经开始的状态，不要求它当前仍 active。 */
    fun latestStartedAtOrBefore(
        instant: Instant,
    ): RelationState? =
        states
            .asSequence()
            .filter { it.validFrom <= instant }
            .maxByOrNull { it.validFrom }

    /** 时间线是否为空。 */
    val isEmpty: Boolean
        get() = states.isEmpty()

    companion object {
        fun of(states: List<RelationState>): TemporalTimeline =
            TemporalTimeline(states.toList())
    }
}
