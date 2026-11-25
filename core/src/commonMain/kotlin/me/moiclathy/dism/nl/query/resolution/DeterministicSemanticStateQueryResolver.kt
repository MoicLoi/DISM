package me.moiclathy.dism.nl.query.resolution

import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.nl.SemanticMention
import me.moiclathy.dism.nl.query.SemanticStateQueryCandidate
import me.moiclathy.dism.nl.resolution.EntityResolutionResult
import me.moiclathy.dism.nl.resolution.EntityResolutionStatus
import me.moiclathy.dism.nl.resolution.DeterministicEntityResolver
import me.moiclathy.dism.nl.resolution.EntityResolver
import kotlin.math.min

/**
 * Resolves entity mentions in a compiled state query into stable Entity IDs.
 * It is deliberately read-only: no entity creation and no state mutation.
 */
class DeterministicSemanticStateQueryResolver(
    private val entityResolver: EntityResolver,
    private val minimumConfidence: Double = 0.85,
) : SemanticStateQueryResolver {
    constructor(
        registry: EntityRegistry,
        minimumConfidence: Double = 0.85,
    ) : this(DeterministicEntityResolver(registry), minimumConfidence)

    init {
        require(minimumConfidence in 0.0..1.0) { "minimumConfidence must be in [0,1]" }
    }

    override fun resolve(candidate: SemanticStateQueryCandidate): SemanticStateQueryResolution {
        val mentions = candidate.unresolvedMentions
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (mentions.isEmpty()) {
            return SemanticStateQueryResolution(
                status = SemanticStateQueryResolutionStatus.UNRESOLVED,
                query = null,
                confidence = 0.0,
                rationale = "Query contains no resolvable entity mention",
            )
        }

        val subject = resolveMention(mentions.first())
        if (subject.status != EntityResolutionStatus.RESOLVED || subject.entityId == null) {
            return SemanticStateQueryResolution(
                status = subject.status.toResolutionStatus(),
                subjectResolution = subject,
                unresolvedMentions = mentions,
                confidence = subjectConfidence(subject),
                rationale = "Subject entity could not be resolved with sufficient confidence",
            )
        }

        val objectResolution = if (mentions.size >= 2) resolveMention(mentions[1]) else null
        if (objectResolution != null && (objectResolution.status != EntityResolutionStatus.RESOLVED || objectResolution.entityId == null)) {
            return SemanticStateQueryResolution(
                status = objectResolution.status.toResolutionStatus(),
                subjectResolution = subject,
                objectResolution = objectResolution,
                unresolvedMentions = mentions.drop(1),
                confidence = min(subjectConfidence(subject), subjectConfidence(objectResolution)),
                rationale = "Object entity could not be resolved with sufficient confidence",
            )
        }

        val resolvedQuery = candidate.query.copy(
            subjectEntityId = subject.entityId.toString(),
            objectEntityId = objectResolution?.entityId?.toString(),
            confidence = min(
                candidate.query.confidence.coerceIn(0.0, 1.0),
                min(subjectConfidence(subject), objectResolution?.let(::subjectConfidence) ?: 1.0),
            ),
        )

        return SemanticStateQueryResolution(
            status = SemanticStateQueryResolutionStatus.RESOLVED,
            query = resolvedQuery,
            subjectResolution = subject,
            objectResolution = objectResolution,
            confidence = resolvedQuery.confidence,
            rationale = "All query entity mentions resolved without mutating semantic state",
        )
    }

    private fun resolveMention(value: String): EntityResolutionResult =
        entityResolver.resolve(SemanticMention(surface = value, normalized = value))

    private fun subjectConfidence(result: EntityResolutionResult): Double =
        result.candidates.firstOrNull()?.score ?: 0.0

    private fun EntityResolutionStatus.toResolutionStatus(): SemanticStateQueryResolutionStatus = when (this) {
        EntityResolutionStatus.RESOLVED -> SemanticStateQueryResolutionStatus.RESOLVED
        EntityResolutionStatus.AMBIGUOUS -> SemanticStateQueryResolutionStatus.AMBIGUOUS
        EntityResolutionStatus.UNRESOLVED -> SemanticStateQueryResolutionStatus.UNRESOLVED
    }
}
