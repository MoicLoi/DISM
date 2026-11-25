package me.moiclathy.dism.attention.context

/**
 * Small deterministic adapter. It does not infer context itself; callers feed it
 * the current semantic context produced by DISM's state/context layer.
 */
class AttentionContextEngine(
    private val policy: AttentionContextPolicy = AttentionContextPolicy(),
) {
    fun evaluate(context: AttentionContext): AttentionContextDecision =
        AttentionContextDecision(
            effectiveBudget = policy.effectiveBudget(context),
            allowNonCritical = policy.allowsNonCritical(context),
            mode = context.mode,
        )
}
