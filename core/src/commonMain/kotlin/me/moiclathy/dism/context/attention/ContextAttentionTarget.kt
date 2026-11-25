package me.moiclathy.dism.context.attention

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context → Attention 的目标。
 *
 * 不直接依赖 Recommendation，
 * 从而避免 9.x context 模块反向耦合 7.x runtime。
 */
data class ContextAttentionTarget(
    val targetId: String,
    val targetType: String,
    val contextMode: ContextMode,
) {

    init {
        require(targetId.isNotBlank()) {
            "targetId must not be blank."
        }

        require(targetType.isNotBlank()) {
            "targetType must not be blank."
        }
    }
}
