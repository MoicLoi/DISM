package me.moiclathy.dism.evidence

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * DISM 中所有状态判断的证据来源。
 *
 * 系统不应该只保存：
 *
 * “充电宝在宿舍”
 *
 * 还应该知道：
 *
 * “为什么我们目前认为充电宝在宿舍？”
 */
@OptIn(ExperimentalUuidApi::class)
data class Evidence(
    val id: Uuid = Uuid.random(),

    /**
     * Evidence 的来源类型。
     */
    val type: EvidenceType,

    /**
     * 原始来源描述。
     *
     * 第一阶段先使用 String，
     * 后续可以升级为真正的 Source / Provenance 模型。
     */
    val source: String,

    /**
     * 这条证据实际发生或被观察到的时间。
     */
    val observedAt: Instant,

    /**
     * 证据本身的可信度。
     *
     * 取值范围：
     * 0.0 .. 1.0
     */
    val confidence: Double = 1.0,
) {
    init {
        require(confidence in 0.0..1.0) {
            "Evidence confidence must be between 0.0 and 1.0"
        }
    }
}