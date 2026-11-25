package me.moiclathy.dism.attention.explanation

/**
 * Pure explanation builder for attention decisions.
 * It intentionally receives already-computed policy inputs rather than re-running policy.
 */
class AttentionExplanationEngine {
    fun explain(
        selected: Boolean,
        basePriority: Double,
        effectivePriority: Double,
        minimumPriority: Double = 0.25,
        criticalPriority: Double = 0.90,
        budgetLimited: Boolean = false,
        contextSuppressed: Boolean = false,
        nonCriticalSuppressed: Boolean = false,
        adaptiveDelta: Double = effectivePriority - basePriority,
    ): AttentionDecisionExplanation {
        val base = basePriority.coerceIn(0.0, 1.0)
        val effective = effectivePriority.coerceIn(0.0, 1.0)
        val reasons = buildList {
            if (contextSuppressed) add(AttentionDecisionReason.CONTEXT_SUPPRESSED)
            if (nonCriticalSuppressed && effective < criticalPriority) {
                add(AttentionDecisionReason.NON_CRITICAL_SUPPRESSED)
            }
            if (effective < minimumPriority) {
                add(AttentionDecisionReason.BELOW_MINIMUM_PRIORITY)
            }
            if (effective >= criticalPriority) {
                add(AttentionDecisionReason.CRITICAL_PRIORITY)
            } else if (effective >= 0.75) {
                add(AttentionDecisionReason.HIGH_PRIORITY)
            }
            if (adaptiveDelta > 0.0001) add(AttentionDecisionReason.ADAPTIVE_BOOST)
            if (adaptiveDelta < -0.0001) add(AttentionDecisionReason.ADAPTIVE_SUPPRESSION)
            if (budgetLimited) {
                add(
                    if (selected) AttentionDecisionReason.SELECTED_BY_BUDGET
                    else AttentionDecisionReason.SOFT_LIMIT_REACHED
                )
            }
            if (!selected && effective >= criticalPriority && budgetLimited) {
                add(AttentionDecisionReason.HARD_LIMIT_REACHED)
            }
            if (isEmpty()) add(AttentionDecisionReason.DEFAULT)
        }

        return AttentionDecisionExplanation(
            selected = selected,
            basePriority = base,
            effectivePriority = effective,
            reasons = reasons.distinct(),
            summary = summarize(selected, reasons),
        )
    }

    private fun summarize(
        selected: Boolean,
        reasons: List<AttentionDecisionReason>,
    ): String {
        if (selected) {
            return when {
                AttentionDecisionReason.CRITICAL_PRIORITY in reasons -> "已进入注意力队列：优先级较高或关键。"
                AttentionDecisionReason.ADAPTIVE_BOOST in reasons -> "已进入注意力队列：历史反馈提高了关注度。"
                else -> "已进入注意力队列：符合当前注意力预算。"
            }
        }

        return when {
            AttentionDecisionReason.CONTEXT_SUPPRESSED in reasons -> "暂不打扰：当前上下文禁止此类提醒。"
            AttentionDecisionReason.NON_CRITICAL_SUPPRESSED in reasons -> "暂不打扰：当前上下文仅保留关键提醒。"
            AttentionDecisionReason.HARD_LIMIT_REACHED in reasons -> "暂不打扰：注意力硬上限已达到。"
            AttentionDecisionReason.BELOW_MINIMUM_PRIORITY in reasons -> "暂不打扰：优先级低于最低阈值。"
            AttentionDecisionReason.ADAPTIVE_SUPPRESSION in reasons -> "暂不打扰：历史反馈降低了关注度。"
            else -> "暂不打扰：当前注意力预算未选择此项。"
        }
    }
}
