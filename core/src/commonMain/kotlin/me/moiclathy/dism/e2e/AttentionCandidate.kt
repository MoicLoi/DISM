package me.moiclathy.dism.e2e

enum class AttentionCandidateStatus { PENDING, ACKNOWLEDGED, RESOLVED, SUPPRESSED }

data class AttentionCandidate(
    val key: String,
    val recommendationKey: String,
    val priority: Double,
    val message: String,
    val status: AttentionCandidateStatus = AttentionCandidateStatus.PENDING,
) {
    init { require(priority in 0.0..1.0) }
}
