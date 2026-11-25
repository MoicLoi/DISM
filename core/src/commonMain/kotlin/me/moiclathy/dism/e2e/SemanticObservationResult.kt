package me.moiclathy.dism.e2e

data class SemanticObservationResult(
    val observations: List<SemanticObservation>,
    val risks: List<RequirementRisk>,
    val recommendations: List<VerticalRecommendation>,
    val attentionCandidates: List<AttentionCandidate>,
)
