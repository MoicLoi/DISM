package me.moiclathy.dism.state.transition

import me.moiclathy.dism.state.model.RelationKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * DISM 中发生的一次状态变化。
 *
 * Transition 描述"现实发生了什么变化"，
 * 具体如何应用到状态，由 StateReducer 负责。
 *
 * effectiveAt：
 * 现实状态实际改变的时间（World Time），
 * 不是 DISM 得知这个消息的时间（Knowledge Time）。
 *
 * 例如用户今天 10:00 说"昨晚 22:47 放宿舍了"：
 * observedAt = 今天 10:00
 * effectiveAt = 昨晚 22:47
 */
sealed interface StateTransition {

    val effectiveAt: Instant
    val evidenceIds: Set<Uuid>

    /**
     * 新建或确认一条关系。
     */
    data class AssertRelation(
        val key: RelationKey,

        /**
         * 这次语义 assertion 的 semantic confidence：
         * 从支持证据中得出这条关系有多可信。
         */
        val semanticConfidence: Double,
        override val effectiveAt: Instant,
        override val evidenceIds: Set<Uuid>,
    ) : StateTransition {
        init {
            require(semanticConfidence in 0.0..1.0) {
                "Transition semantic confidence must be between 0.0 and 1.0"
            }
        }
    }

    /**
     * 新关系取代旧关系。
     *
     * 例如：
     * 充电宝 located_at 宿舍
     * →
     * 充电宝 inside 背包
     */
    data class SupersedeRelation(
        val oldKey: RelationKey,
        val newKey: RelationKey,
        val semanticConfidence: Double,
        override val effectiveAt: Instant,
        override val evidenceIds: Set<Uuid>,
    ) : StateTransition {
        init {
            require(semanticConfidence in 0.0..1.0) {
                "Transition semantic confidence must be between 0.0 and 1.0"
            }
        }
    }

    /**
     * 明确否定一条关系。
     *
     * 例如：
     * "不对，我昨晚根本没把充电宝放宿舍。"
     */
    data class InvalidateRelation(
        val key: RelationKey,
        override val effectiveAt: Instant,
        override val evidenceIds: Set<Uuid>,
    ) : StateTransition
}
