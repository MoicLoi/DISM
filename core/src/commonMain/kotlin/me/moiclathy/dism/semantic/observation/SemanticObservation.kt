package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.domain.relation.Predicate
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 一次已完成语义解析的观察。
 *
 * Observation 是外部世界告诉 DISM 的事实，
 * 不包含 source / rawText / reason ——
 * 这些属于 Evidence。
 *
 * 同一个 Evidence 可以产生多个 Observation。
 * 例如："我把校园卡和充电宝都放包里了"
 * → 一个 Evidence，两个 Observation。
 */
data class SemanticObservation(
    val subjectId: Uuid,
    val predicate: Predicate,
    val objectId: Uuid,

    val mode: ObservationMode = ObservationMode.ASSERT,

    val confidence: Double,

    val evidenceIds: Set<Uuid>,

    /**
     * DISM 获得/观察到这条信息的时间。
     */
    val observedAt: Instant,

    /**
     * 关系真正生效的时间。
     *
     * 默认和 observedAt 相同；
     * 回溯性陈述（"昨晚 22:47 放宿舍了"）时才不同。
     */
    val effectiveAt: Instant = observedAt,
) {
    init {
        require(confidence in 0.0..1.0) {
            "Observation confidence must be between 0.0 and 1.0"
        }
    }
}
