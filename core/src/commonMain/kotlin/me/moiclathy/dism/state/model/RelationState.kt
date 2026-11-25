package me.moiclathy.dism.state.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * DISM 中的一条动态关系状态。
 *
 * RelationState 描述：
 *
 * 1. 谁
 * 2. 与谁
 * 3. 存在什么关系
 * 4. 当前是否仍有效
 * 5. 为什么我们相信它
 * 6. 从什么时候开始成立
 *
 * confidence 是派生值：
 * 由 supports（每份 Evidence 的语义支持）经
 * RelationConfidenceAggregator 计算得出，
 * 不是调用者直接塞进来的最终答案。
 */
data class RelationState(
    val key: RelationKey,

    /**
     * 综合所有支持证据之后的当前置信度。
     *
     * 注意：
     * 这是状态本身的置信度，
     * 不一定等同于单条 Evidence 的 confidence。
     */
    val confidence: Double,

    /**
     * 每份 Evidence 对当前关系的语义支持。
     *
     * 这是证据链的唯一真相来源；
     * evidenceIds 只是它的派生视图。
     */
    val supports: Set<RelationSupport>,

    /**
     * 该状态开始成立的时间。
     */
    val validFrom: Instant,

    /**
     * 状态不再成立的时间。
     *
     * null 表示当前仍未确定结束。
     */
    val validUntil: Instant? = null,

    val status: RelationStatus = RelationStatus.ACTIVE,
) {
    init {
        require(confidence in 0.0..1.0) {
            "RelationState confidence must be between 0.0 and 1.0"
        }

        require(validUntil == null || validUntil >= validFrom) {
            "validUntil cannot be earlier than validFrom"
        }
    }

    /**
     * 支持当前状态的 Evidence ID 集合（派生自 supports）。
     */
    val evidenceIds: Set<Uuid>
        get() = supports
            .mapTo(mutableSetOf()) {
                it.evidenceId
            }

    val isActive: Boolean
        get() = status == RelationStatus.ACTIVE && validUntil == null

    /**
     * 该状态在某个时间点上是否有效。
     *
     * 采用 [validFrom, validUntil) 左闭右开区间：
     * 07:00–08:00 的状态在 07:59:59 有效，08:00:00 不再有效。
     *
     * 注意：不检查 status ——
     * 一个已经 SUPERSEDED 的历史状态，在它的历史时间段里
     * 仍然是真实有效的。
     *
     * isActive（当前仍成立）与 isValidAt(time)（当时成立）
     * 是两个不同概念。
     */
    fun isValidAt(
        instant: Instant,
    ): Boolean {
        if (instant < validFrom) {
            return false
        }

        if (
            validUntil != null &&
            instant >= validUntil
        ) {
            return false
        }

        return true
    }
}
