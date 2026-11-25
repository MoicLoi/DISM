package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * 测试/无持久化场景的提交器。
 *
 * 生产 SQLDelight 实现使用真正的单数据库事务；
 * 这里复用 Repository API，保持默认 SemanticStateEngine 构造兼容。
 *
 * states 是完整世界状态（全量替换）；
 * transitions 是本次新增记录——transition log 是 append-only 历史，
 * 因此提交时取「旧历史 + 本次新增」一次性批量替换，
 * 而不是逐条 append（避免中途失败留下半成品），也不是用本次记录覆盖全部历史。
 */
class InMemoryStateCommitRepository(
    private val stateRepository: RelationStateRepository,
    private val transitionLogRepository: TransitionLogRepository,
) : StateCommitRepository {

    override fun commit(
        states: List<RelationState>,
        transitions: List<StateTransitionRecord>,
    ) {
        val mergedTransitions =
            transitionLogRepository.all() + transitions

        stateRepository.replaceAll(states)
        transitionLogRepository.replaceAll(mergedTransitions)
    }
}
