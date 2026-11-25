package me.moiclathy.dism.semantic.resolution

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 一次对实体的最近历史引用。
 *
 * 只保存"引用过谁、什么时候、有多可靠"。
 * 不保存原始句子 —— 原始文本由 Evidence / conversation source 管理。
 */
data class RecentEntityReference(
    val entityId: Uuid,
    val referencedAt: Instant,

    /**
     * 这次历史引用本身有多可靠。
     */
    val confidence: Double = 1.0,
) {
    init {
        require(confidence in 0.0..1.0) {
            "Recent entity reference confidence must be between 0.0 and 1.0"
        }
    }
}
