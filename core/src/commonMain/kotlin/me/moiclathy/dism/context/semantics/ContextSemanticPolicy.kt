package me.moiclathy.dism.context.semantics

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Semantic Policy。
 *
 * 负责控制：
 *
 * - 默认语义权重
 * - Context Confidence 的影响程度
 * - 最终权重上下界
 */
data class ContextSemanticPolicy(

    /**
     * 没有匹配规则时的基础权重。
     */
    val defaultWeight: Double = 0.50,

    /**
     * Context Confidence 对语义影响的最大程度。
     *
     * 例如：
     *
     * weightDelta = 0.40
     * confidence = 0.50
     *
     * 实际增加：
     *
     * 0.20
     */
    val confidenceMultiplier: Double = 1.0,

    /**
     * 最低语义权重。
     */
    val minimumWeight: Double = 0.0,

    /**
     * 最高语义权重。
     */
    val maximumWeight: Double = 1.0,

    /**
     * 默认规则。
     */
    val rules: List<ContextSemanticRule> =
        defaultRules(),
) {

    init {
        require(defaultWeight in 0.0..1.0) {
            "defaultWeight must be between 0.0 and 1.0."
        }

        require(confidenceMultiplier >= 0.0) {
            "confidenceMultiplier must not be negative."
        }

        require(minimumWeight in 0.0..1.0) {
            "minimumWeight must be between 0.0 and 1.0."
        }

        require(maximumWeight in 0.0..1.0) {
            "maximumWeight must be between 0.0..1.0."
        }

        require(minimumWeight <= maximumWeight) {
            "minimumWeight must not exceed maximumWeight."
        }
    }

    companion object {

        fun defaultRules(): List<ContextSemanticRule> =
            listOf(

                ContextSemanticRule(
                    contextMode = ContextMode.IN_CLASS,
                    targetType = "EVENT",
                    relation =
                        ContextSemanticRelation.RELEVANT,
                    weightDelta = 0.30,
                    reason =
                        "The user is currently in class.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.IN_CLASS,
                    targetType = "ITEM",
                    relation =
                        ContextSemanticRelation.RELEVANT,
                    weightDelta = 0.25,
                    reason =
                        "Items related to the current class are more relevant.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.IN_TRANSIT,
                    targetType = "LOCATION",
                    relation =
                        ContextSemanticRelation.RELEVANT,
                    weightDelta = 0.25,
                    reason =
                        "Location information is more relevant while travelling.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.IN_TRANSIT,
                    targetType = "EVENT",
                    relation =
                        ContextSemanticRelation.SUPPORTING,
                    weightDelta = 0.15,
                    reason =
                        "Upcoming events may affect the current route.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.SLEEPING,
                    targetType = "REMINDER",
                    relation =
                        ContextSemanticRelation.SUPPRESSING,
                    weightDelta = -0.60,
                    reason =
                        "Non-critical reminders should be suppressed while sleeping.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.SLEEPING,
                    targetType = "EVENT",
                    relation =
                        ContextSemanticRelation.IRRELEVANT,
                    weightDelta = -0.20,
                    reason =
                        "Ordinary events are less relevant while sleeping.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.EMERGENCY,
                    targetType = "EVENT",
                    relation =
                        ContextSemanticRelation.RELEVANT,
                    weightDelta = 0.40,
                    reason =
                        "Events may require immediate reevaluation during emergencies.",
                ),

                ContextSemanticRule(
                    contextMode = ContextMode.EMERGENCY,
                    targetType = "REMINDER",
                    relation =
                        ContextSemanticRelation.SUPPORTING,
                    weightDelta = 0.20,
                    reason =
                        "Relevant reminders can support emergency replanning.",
                ),
            )
    }
}
