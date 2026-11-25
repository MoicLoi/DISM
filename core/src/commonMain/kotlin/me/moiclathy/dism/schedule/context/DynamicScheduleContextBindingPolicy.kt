package me.moiclathy.dism.schedule.context

import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleContextBindingPolicy(
    /**
     * Bindings below this effective confidence are ignored.
     */
    val minimumEffectiveConfidence: Double = 0.05,

    /**
     * If true, an already BLOCKED item is treated as explicitly blocked
     * and context evaluation will not automatically release it.
     *
     * This prevents a semantic context projection from silently
     * overriding an explicit state decision.
     */
    val preserveExplicitBlockedState: Boolean = true,
) {
    init {
        require(minimumEffectiveConfidence in 0.0..1.0) {
            "minimumEffectiveConfidence must be in [0, 1]"
        }
    }
}
