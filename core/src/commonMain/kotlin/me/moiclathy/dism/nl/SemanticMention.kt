package me.moiclathy.dism.nl

/** A span/mention candidate. It is not yet an Entity. */
data class SemanticMention(
    val surface: String,
    val normalized: String = surface.trim(),
    val typeHint: String? = null,
    val confidence: Double = 1.0,
) {
    init {
        require(surface.isNotBlank()) { "surface must not be blank" }
        require(normalized.isNotBlank()) { "normalized must not be blank" }
        require(confidence in 0.0..1.0) { "confidence must be in [0,1]" }
    }
}
