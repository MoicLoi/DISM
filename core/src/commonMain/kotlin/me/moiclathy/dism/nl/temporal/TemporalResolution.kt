package me.moiclathy.dism.nl.temporal

data class TemporalResolution(
    val mentions: List<TemporalMention>,
    val confidence: Double,
    val unresolvedText: String? = null,
) {
    init { require(confidence in 0.0..1.0) }
}
