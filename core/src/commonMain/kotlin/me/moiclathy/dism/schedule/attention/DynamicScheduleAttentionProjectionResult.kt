package me.moiclathy.dism.schedule.attention

/**
 * Complete result of one schedule-to-attention projection.
 */
data class DynamicScheduleAttentionProjectionResult(
    val candidates: List<DynamicScheduleAttentionCandidate>,

    /**
     * Schedule items that were intentionally not projected.
     */
    val skippedItemIds: List<String> = emptyList(),

    /**
     * Human-readable diagnostic reasons.
     *
     * These are diagnostics, not user-facing notification messages.
     */
    val diagnostics: List<String> = emptyList(),
) {
    val candidateCount: Int
        get() = candidates.size

    val skippedCount: Int
        get() = skippedItemIds.size
}
