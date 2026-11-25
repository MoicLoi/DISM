package me.moiclathy.dism.state.constraint

import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates

/**
 * 定义关系谓词到语义槽的映射。
 *
 * 属于同一槽位的谓词在同一主体上通常互斥。
 */
class RelationConstraintPolicy {

    fun slotOf(
        predicate: Predicate,
    ): RelationSlot? {
        return when (predicate) {
            Predicates.LOCATED_AT,
            Predicates.INSIDE,
            Predicates.CARRIED_BY ->
                RelationSlot.PHYSICAL_LOCATION

            else -> null
        }
    }

    fun conflicts(
        firstPredicate: Predicate,
        secondPredicate: Predicate,
    ): Boolean {
        val firstSlot =
            slotOf(firstPredicate)
                ?: return false

        val secondSlot =
            slotOf(secondPredicate)
                ?: return false

        return firstSlot == secondSlot
    }
}
