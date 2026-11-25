package me.moiclathy.dism.state.query

import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Instant

/**
 * 统一的状态读取层。
 *
 * 现在看起来只是 filter() 包装，
 * 但这个 abstraction 值得保留——
 * 以后可以在这里统一处理：
 *
 * - validity at time（Temporal Query）
 * - minimum confidence
 * - inferred vs fact
 * - graph traversal
 * - historical state
 * - uncertainty
 *
 * 而不需要让所有调用方各写一套过滤。
 */
class StateQueryService {

    fun query(
        states: List<RelationState>,
        query: RelationQuery,
    ): List<RelationState> {
        return states.filter { state ->

            if (
                !matchesTemporalScope(
                    state = state,
                    scope = query.temporalScope,
                )
            ) {
                return@filter false
            }

            if (
                query.subjectId != null &&
                state.key.subjectId !=
                query.subjectId
            ) {
                return@filter false
            }

            if (
                query.predicate != null &&
                state.key.predicate !=
                query.predicate
            ) {
                return@filter false
            }

            if (
                query.objectId != null &&
                state.key.objectId !=
                query.objectId
            ) {
                return@filter false
            }

            if (
                state.confidence <
                query.minimumConfidence
            ) {
                return@filter false
            }

            true
        }
    }

    fun firstOrNull(
        states: List<RelationState>,
        query: RelationQuery,
    ): RelationState? {
        return query(
            states = states,
            query = query,
        ).firstOrNull()
    }

    /**
     * 为一个 subject + predicate 构造只读时间线。
     *
     * 这是 Temporal State 的统一读取入口；调用方不需要自己对
     * RelationState 列表排序或重复实现时间区间判断。
     */
    fun timeline(
        states: List<RelationState>,
        subjectId: kotlin.uuid.Uuid,
        predicate: me.moiclathy.dism.domain.relation.Predicate,
    ): me.moiclathy.dism.state.temporal.TemporalTimeline {
        return me.moiclathy.dism.state.temporal.TemporalTimeline.of(
            states.filter {
                it.key.subjectId == subjectId &&
                    it.key.predicate == predicate
            }
        )
    }

    /**
     * 某个时间点发生了哪些状态切换。
     *
     * Started / Ended 区分开：
     * 例如 22:47 一条状态结束、另一条状态开始。
     */
    fun changesAt(
        states: List<RelationState>,
        instant: Instant,
    ): List<TemporalStateChange> {

        return buildList {
            states.forEach { state ->

                if (state.validUntil == instant) {
                    add(
                        TemporalStateChange.Ended(
                            state
                        )
                    )
                }

                if (state.validFrom == instant) {
                    add(
                        TemporalStateChange.Started(
                            state
                        )
                    )
                }
            }
        }
    }

    private fun matchesTemporalScope(
        state: RelationState,
        scope: TemporalScope,
    ): Boolean {
        return when (scope) {

            TemporalScope.Current ->
                state.isActive

            is TemporalScope.At ->
                state.isValidAt(
                    scope.instant
                )

            TemporalScope.All ->
                true
        }
    }
}
