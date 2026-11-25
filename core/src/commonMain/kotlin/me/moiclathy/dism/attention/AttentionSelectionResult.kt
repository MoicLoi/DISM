package me.moiclathy.dism.attention

import me.moiclathy.dism.planning.recommendation.Recommendation

/**
 * Explains how the current attention budget was spent.
 *
 * This is deliberately a pure value object. It does not record that the user
 * actually saw anything; delivery state remains the responsibility of the
 * Attention lifecycle layer.
 */
data class AttentionSelectionResult(
    val selected: List<Recommendation>,
    val suppressed: List<Recommendation>,
    val softLimitReached: Boolean,
    val hardLimitReached: Boolean,
)
