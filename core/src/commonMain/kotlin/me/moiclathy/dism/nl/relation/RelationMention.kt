package me.moiclathy.dism.nl.relation

import me.moiclathy.dism.nl.SemanticMention

/** A relation candidate between two semantic mentions. It is not yet a state transition. */
data class RelationMention(
    val subject: SemanticMention,
    val predicateSurface: String,
    val `object`: SemanticMention,
    val confidence: Double = 1.0,
) {
    init { require(confidence in 0.0..1.0) { "confidence must be in [0,1]" } }
}
