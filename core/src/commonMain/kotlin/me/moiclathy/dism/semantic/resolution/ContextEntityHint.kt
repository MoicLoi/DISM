package me.moiclathy.dism.semantic.resolution

import kotlin.uuid.Uuid

/**
 * 上下文对特定实体的相关性提示。
 *
 * 用于 Entity Resolution 时，当词汇匹配分数相近时，
 * 上下文可以帮助打破平局。
 */
data class ContextEntityHint(
    val entityId: Uuid,

    /**
     * 当前上下文对这个实体的额外相关性。
     *
     * 0.0 = 无额外帮助
     * 1.0 = 极强上下文指向
     */
    val relevance: Double,
) {
    init {
        require(relevance in 0.0..1.0) {
            "Context entity relevance must be between 0.0 and 1.0"
        }
    }
}
