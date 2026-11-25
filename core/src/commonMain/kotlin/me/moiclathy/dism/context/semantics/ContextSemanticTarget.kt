package me.moiclathy.dism.context.semantics

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context 依赖语义作用的目标。
 *
 * targetId 可以对应：
 *
 * Entity
 * Event
 * Relation
 * Recommendation
 * 其他未来语义对象
 *
 * 这里故意不直接依赖具体 Entity / Event 类型，
 * 避免 Context 模块反向耦合整个 semantic domain。
 */
data class ContextSemanticTarget(
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
