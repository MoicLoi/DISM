package me.moiclathy.dism.context.recommendation

import me.moiclathy.dism.context.model.ContextMode

/**
 * Contextual Recommendation 的目标。
 *
 * targetId 可以对应：
 *
 * - Event
 * - Item
 * - Location
 * - Requirement
 * - Reminder
 *
 * targetType 保持字符串形式，
 * 避免这里与具体 Recommendation Domain 强耦合。
 */
data class ContextualRecommendationTarget(
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
