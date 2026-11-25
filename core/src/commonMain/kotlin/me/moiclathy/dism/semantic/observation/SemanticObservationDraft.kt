package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 已经完成"语义解析"，但还没有完成实体绑定的 Observation。
 *
 * 例如：
 *
 * "充电宝我带上了"
 *
 * 会先解析为：
 *
 * subjectMention = EntityMention("充电宝")
 * predicate = carried_by
 * objectMention = EntityMention("我")
 *
 * 而不是直接要求调用方知道 Entity UUID。
 */
data class SemanticObservationDraft(
    val subjectMention: EntityMention,
    val predicate: Predicate,
    val objectMention: EntityMention,

    val mode: ObservationMode = ObservationMode.ASSERT,

    val confidence: Double,

    val evidenceIds: Set<Uuid>,

    /**
     * DISM 获得/观察到这条信息的时间。
     */
    val observedAt: Instant,

    /**
     * 这条语义在现实中实际开始成立的时间。
     *
     * 默认和 observedAt 相同——
     * 绝大多数即时陈述（"我出门了"、"钥匙带了"）
     * 都属于这一档。
     *
     * 只有明确回溯时才不同：
     * "昨晚 22:47 我把充电宝放宿舍了"
     * observedAt = 今天 10:00
     * effectiveAt = 昨晚 22:47
     */
    val effectiveAt: Instant = observedAt,
) {
    init {
        require(confidence in 0.0..1.0) {
            "Observation draft confidence must be between 0.0 and 1.0"
        }
    }
}
