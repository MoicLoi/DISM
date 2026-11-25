package me.moiclathy.dism.planning.recommendation

/**
 * Language-neutral semantic messages produced by the core.
 *
 * The presentation layer (app) is responsible for rendering
 * these into localized text, natural-language expressions,
 * or any other output format.
 */
sealed interface RecommendationMessage {

    data class PrepareItemKnownLocation(
        val eventName: String,
        val itemName: String,
        val locationName: String,
    ) : RecommendationMessage

    data class PrepareItemUnknownLocation(
        val eventName: String,
        val itemName: String,
    ) : RecommendationMessage

    /**
     * Fallback for recommendation types that do not yet
     * have a dedicated message type.
     */
    data class Generic(
        val key: String,
    ) : RecommendationMessage
}
