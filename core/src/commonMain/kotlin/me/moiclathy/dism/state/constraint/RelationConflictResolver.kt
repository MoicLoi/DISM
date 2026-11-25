package me.moiclathy.dism.state.constraint

import me.moiclathy.dism.state.model.RelationState

/**
 * 当新关系状态到来时，自动发现会被取代的旧状态。
 *
 * 这是 DISM 从"上层必须手动指定被替代关系"
 * 走向"系统自动推断冲突"的核心机制。
 */
class RelationConflictResolver(
    private val policy: RelationConstraintPolicy =
        RelationConstraintPolicy(),
) {

    /**
     * 找出会被 incomingState 替代的 ACTIVE 状态。
     */
    fun findConflicts(
        states: List<RelationState>,
        incomingState: RelationState,
    ): List<RelationState> {
        return states.filter { existing ->

            existing.isActive &&

                // 必须描述同一个 subject。
                existing.key.subjectId ==
                incomingState.key.subjectId &&

                // 必须属于互斥语义槽。
                policy.conflicts(
                    existing.key.predicate,
                    incomingState.key.predicate,
                ) &&

                // 完全相同的 Relation 不视为冲突。
                existing.key != incomingState.key
        }
    }
}
