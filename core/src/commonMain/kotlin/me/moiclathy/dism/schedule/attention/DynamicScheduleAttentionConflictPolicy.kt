package me.moiclathy.dism.schedule.attention

import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleAttentionConflictPolicy(
    /**
     * Hard constraints always preserve at least this priority.
     */
    val minimumHardConstraintPriority: Double = 0.75,

    /**
     * Explicit user requests are even stronger.
     */
    val explicitRequestPriority: Double = 1.0,

    /**
     * Maximum amount learned memory may suppress
     * a normal schedule priority.
     */
    val maximumLearnedSuppression: Double = 0.5,

    /**
     * Whether explicit user requests may override everything
     * except impossible/invalid states.
     */
    val allowExplicitOverride: Boolean = true,
) {
    init {
        require(minimumHardConstraintPriority in 0.0..1.0)
        require(explicitRequestPriority in 0.0..1.0)
        require(maximumLearnedSuppression in 0.0..1.0)
    }
}
