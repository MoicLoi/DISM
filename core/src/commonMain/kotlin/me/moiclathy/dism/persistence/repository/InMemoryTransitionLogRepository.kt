package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.time.Instant

/**
 * 第一版内存实现，主要给 Core 测试使用。
 *
 * at() 第一版采用精确时间匹配；
 * 以后需要范围查询再加 between(from, until)，不提前复杂化 API。
 */
class InMemoryTransitionLogRepository :
    TransitionLogRepository {

    private val records =
        mutableListOf<StateTransitionRecord>()

    override fun append(
        record: StateTransitionRecord,
    ) {
        records += record
    }

    override fun all():
        List<StateTransitionRecord> =
        records.toList()

    override fun at(
        instant: Instant,
    ): List<StateTransitionRecord> =
        records.filter {
            it.effectiveAt == instant
        }

    /**
     * 单次批量替换——配合 InMemoryStateCommitRepository
     * 让状态与 transition log 成为一次批量提交。
     */
    override fun replaceAll(
        records: List<StateTransitionRecord>,
    ) {
        this.records.clear()
        this.records.addAll(records)
    }
}
