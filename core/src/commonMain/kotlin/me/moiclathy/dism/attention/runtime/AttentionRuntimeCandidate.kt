package me.moiclathy.dism.attention.runtime

import me.moiclathy.dism.planning.recommendation.Recommendation

/** A recommendation together with the stable attention identity required by the runtime. */
data class AttentionRuntimeCandidate(
    val recommendation: Recommendation,
    val attentionKey: String,
) {
    init { require(attentionKey.isNotBlank()) }
}
