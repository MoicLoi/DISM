package me.moiclathy.dism.runtime

import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * 面向 App 的长期状态只读快照。
 *
 * 与持久化层 / RestoredCoreState 解耦：
 * 以后数据库结构如何演进，App 看到的形状保持稳定。
 */
@DismPublicApi
data class RuntimeSnapshot(
    val entities: List<Entity>,
    val evidence: List<Evidence>,
    val relationStates: List<RelationState>,
    val transitions: List<StateTransitionRecord>,
)
