package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import kotlin.time.Instant

/**
 * 第一版内存实现，主要给 Core 测试使用。
 */
class InMemoryRelationStateRepository :
    RelationStateRepository {

    private var states:
        List<RelationState> =
        emptyList()

    override fun loadAll():
        List<RelationState> {
        return states
    }

    override fun replaceAll(
        states: List<RelationState>,
    ) {
        this.states =
            states.toList()
    }

    override fun queryActiveAt(
        instant: Instant,
    ): List<RelationState> {
        return states.filter { state ->
            state.status == RelationStatus.ACTIVE &&
                state.validFrom <= instant &&
                (state.validUntil == null || instant < state.validUntil)
        }
    }
}
