package me.moiclathy.dism.e2e

/**
 * Small deterministic vertical-slice coordinator.
 * It is an integration seam, not a replacement for the existing domain engines.
 */
class SemanticAttentionVerticalSlice {
    private val observations = linkedMapOf<String, SemanticObservation>()
    private val risks = linkedMapOf<String, RequirementRisk>()
    private val recommendations = linkedMapOf<String, VerticalRecommendation>()
    private val attention = linkedMapOf<String, AttentionCandidate>()

    fun observe(observation: SemanticObservation): SemanticObservationResult {
        observations[observation.observationId] = observation
        reconcile()
        return snapshot()
    }

    fun observeAll(items: Iterable<SemanticObservation>): SemanticObservationResult {
        items.forEach { observations[it.observationId] = it }
        reconcile()
        return snapshot()
    }

    fun applyFeedback(feedback: AttentionFeedback): SemanticObservationResult {
        val candidate = attention[feedback.attentionKey] ?: return snapshot()
        if (candidate.recommendationKey != feedback.recommendationKey) return snapshot()

        when (feedback.type) {
            AttentionFeedbackType.COMPLETED -> {
                attention[feedback.attentionKey] = candidate.copy(status = AttentionCandidateStatus.RESOLVED)
                recommendations[feedback.recommendationKey]?.let {
                    recommendations[feedback.recommendationKey] = it.copy(status = VerticalRecommendationStatus.RESOLVED)
                    risks[it.riskKey]?.let { risk -> risks[it.riskKey] = risk.copy(status = RequirementRiskStatus.RESOLVED) }
                }
            }
            AttentionFeedbackType.OPENED -> attention[feedback.attentionKey] = candidate.copy(status = AttentionCandidateStatus.ACKNOWLEDGED)
            AttentionFeedbackType.DISMISSED -> attention[feedback.attentionKey] = candidate.copy(status = AttentionCandidateStatus.SUPPRESSED)
            AttentionFeedbackType.SNOOZED, AttentionFeedbackType.IGNORED -> Unit
        }
        return snapshot()
    }

    fun snapshot(): SemanticObservationResult = SemanticObservationResult(
        observations = observations.values.toList(),
        risks = risks.values.toList(),
        recommendations = recommendations.values.toList(),
        attentionCandidates = attention.values.toList(),
    )

    private fun reconcile() {
        val event = observations.values.firstOrNull { it.predicate == "REQUIRES" } ?: return
        val location = observations.values.firstOrNull {
            it.subject == event.objectValue && it.predicate == "LOCATED_AT"
        }

        val riskKey = "${event.subject}:${event.objectValue}:MISSING"
        val recommendationKey = "$riskKey:TAKE"
        val attentionKey = "attention:$recommendationKey"

        val confidence = listOf(event.confidence, location?.confidence ?: 0.5).minOrNull() ?: 0.5
        risks[riskKey] = risks[riskKey]?.takeIf { it.status == RequirementRiskStatus.RESOLVED }
            ?: RequirementRisk(riskKey, event.subject, event.objectValue, confidence = confidence)

        val risk = risks[riskKey]!!
        if (risk.status != RequirementRiskStatus.ACTIVE) return

        recommendations[recommendationKey] = VerticalRecommendation(
            key = recommendationKey,
            riskKey = riskKey,
            eventKey = event.subject,
            message = "出门前带${event.objectValue}",
            priority = (0.5 + confidence * 0.5).coerceIn(0.0, 1.0),
        )
        attention[attentionKey] = AttentionCandidate(
            key = attentionKey,
            recommendationKey = recommendationKey,
            priority = recommendations[recommendationKey]!!.priority,
            message = recommendations[recommendationKey]!!.message,
        )
    }
}
