package me.moiclathy.dism.attention.context

/** Pure policy: converts context into an effective attention budget. */
data class AttentionContextPolicy(
    val normalBudget: Int = 5,
    val hardLimit: Int = 8,
    val focusedBudget: Int = 3,
    val inTransitBudget: Int = 2,
    val inClassBudget: Int = 1,
    val restingBudget: Int = 3,
    val sleepingBudget: Int = 0,
    val emergencyBudget: Int = 8,
) {
    fun effectiveBudget(context: AttentionContext): Int {
        val base = when (context.mode) {
            AttentionContextMode.NORMAL -> normalBudget
            AttentionContextMode.FOCUSED -> focusedBudget
            AttentionContextMode.IN_TRANSIT -> inTransitBudget
            AttentionContextMode.IN_CLASS -> inClassBudget
            AttentionContextMode.RESTING -> restingBudget
            AttentionContextMode.SLEEPING -> sleepingBudget
            AttentionContextMode.EMERGENCY -> emergencyBudget
        }

        val contextLimit = context.maxVisibleItems ?: Int.MAX_VALUE
        return (base * context.attentionMultiplier)
            .toInt()
            .coerceIn(0, minOf(hardLimit, contextLimit))
    }

    fun allowsNonCritical(context: AttentionContext): Boolean =
        !context.suppressNonCritical && context.mode != AttentionContextMode.SLEEPING
}
