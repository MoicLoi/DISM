package me.moiclathy.dism.presentation.recommendation

/**
 * UI-ready text for a [Recommendation].
 *
 * This is the presentation counterpart of the language-neutral
 * `RecommendationMessage` produced by core.
 */
data class RecommendationText(
    val title: String,
    val message: String,
)
