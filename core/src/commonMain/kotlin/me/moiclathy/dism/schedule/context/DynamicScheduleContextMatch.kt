package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleContextMatch(
    val itemId: DynamicScheduleItemId,

    /**
     * True when every relevant REQUIRED binding is currently satisfied.
     */
    val requiredSatisfied: Boolean,

    /**
     * True when at least one relevant EXCLUDED binding is currently active.
     */
    val excludedSatisfied: Boolean,

    /**
     * Deterministic score in [0, 1].
     *
     * A value of 1 means all preferred context weight is currently matched.
     */
    val preferredScore: Double,

    /**
     * Context keys that are currently matched.
     */
    val matchedContextKeys: Set<String>,

    /**
     * Required context keys that are currently missing.
     */
    val unmatchedRequiredContextKeys: Set<String>,

    /**
     * Excluded context keys that are currently active.
     */
    val matchedExcludedContextKeys: Set<String>,
) {
    /**
     * Whether the current context is semantically acceptable.
     */
    val contextSatisfied: Boolean
        get() = requiredSatisfied && !excludedSatisfied
}
