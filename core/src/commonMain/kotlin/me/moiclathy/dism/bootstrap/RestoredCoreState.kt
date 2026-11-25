package me.moiclathy.dism.bootstrap

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * 从持久化层恢复出的完整长期状态。
 *
 * 启动恢复与业务引擎初始化是两个不同职责：
 * CoreBootstrapper 只负责"读出来"，
 * 之后由 Runtime 层把 RestoredCoreState 注入 SemanticStateEngine。
 */
data class RestoredCoreState(
    val entities: List<Entity>,
    val evidence: List<Evidence>,
    val relationStates: List<RelationState>,
    val transitions: List<StateTransitionRecord>,
)
