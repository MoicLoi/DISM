package me.moiclathy.dism.nl

data class SemanticIntent(
    val type: SemanticIntentType,
    val confidence: Double,
    val mentions: List<SemanticMention> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be in [0,1]" }
    }
}
