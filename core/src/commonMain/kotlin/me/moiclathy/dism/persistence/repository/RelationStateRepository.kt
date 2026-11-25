package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Instant

/**
 * 世界状态持久化边界。
 *
 * 第一版故意只提供：
 * - loadAll()
 * - replaceAll(states)
 * - queryActiveAt(instant)
 *
 * 而不是 insert / update / delete / queryBySubject / queryByPredicate。
 *
 * 原因：StateReducer / TemporalReconciler 的核心仍然基于
 * List<RelationState>，MVP 数据量又极小。
 *
 * 最安全的接入方式是：
 * 启动： repository.loadAll()
 * 一次成功 observe： replaceAll(states)
 *
 * 先确保语义与持久化一致，再优化增量 SQL。
 */
interface RelationStateRepository {

    fun loadAll(): List<RelationState>

    fun replaceAll(
        states: List<RelationState>,
    )

    /**
     * 在某个时间点上处于 ACTIVE 的状态：
     * status = ACTIVE
     * validFrom <= T < validUntil（validUntil 为 null 视为永续）。
     *
     * 只返回当前（在该时刻）仍成立的状态，
     * 已被 SUPERSEDED / INVALIDATED 的历史状态不会出现。
     */
    fun queryActiveAt(
        instant: Instant,
    ): List<RelationState>
}
