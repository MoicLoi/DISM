package me.moiclathy.dism.state.query

import me.moiclathy.dism.domain.relation.Predicate
import kotlin.uuid.Uuid

/**
 * 对 RelationState 集合的结构化查询。
 *
 * 所有字段都是可选过滤条件：
 * null 表示不限制该维度。
 *
 * 例如：
 * RelationQuery(subjectId = powerBankId, predicate = Predicates.LOCATED_AT)
 * → 充电宝当前 located_at 谁。
 *
 * RelationQuery(predicate = Predicates.CARRIED_BY, objectId = userId)
 * → 所有当前由用户携带的实体。
 *
 * temporalScope 控制时间维度：
 * - Current：只返回当前仍有效的状态
 * - At(instant)：返回某一时刻有效的状态（含已 supersede 的历史）
 * - All：不限制时间，包含全部历史
 */
data class RelationQuery(
    val subjectId: Uuid? = null,
    val predicate: Predicate? = null,
    val objectId: Uuid? = null,

    val temporalScope: TemporalScope =
        TemporalScope.Current,

    /**
     * 只返回 confidence >= minimumConfidence 的状态。
     *
     * 对 Attention / notification 特别有价值：
     * “只告诉我我们至少有 80% 把握的当前状态。”
     */
    val minimumConfidence: Double = 0.0,
) {
    init {
        require(minimumConfidence in 0.0..1.0) {
            "Minimum confidence must be between 0.0 and 1.0"
        }
    }
}
