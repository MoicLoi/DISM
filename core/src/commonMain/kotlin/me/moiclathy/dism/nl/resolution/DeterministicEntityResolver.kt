package me.moiclathy.dism.nl.resolution

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.nl.SemanticMention

/**
 * Deterministic, local-first entity resolution.
 * It never creates or mutates entities and never changes semantic state.
 */
class DeterministicEntityResolver(
    private val registry: EntityRegistry,
    private val resolveThreshold: Double = 0.85,
    private val ambiguityMargin: Double = 0.10,
) : EntityResolver {
    init {
        require(resolveThreshold in 0.0..1.0)
        require(ambiguityMargin >= 0.0)
    }

    override fun resolve(mention: SemanticMention): EntityResolutionResult {
        val normalized = normalize(mention.normalized)
        if (normalized.isBlank()) return EntityResolutionResult(mention, EntityResolutionStatus.UNRESOLVED)

        val candidates = registry.all().mapNotNull { entity -> score(entity, normalized, mention) }
            .sortedWith(compareByDescending<EntityResolutionCandidate> { it.score }.thenBy { it.entity.id.toString() })
            .take(8)

        val best = candidates.firstOrNull()
            ?: return EntityResolutionResult(mention, EntityResolutionStatus.UNRESOLVED)

        val second = candidates.getOrNull(1)
        val ambiguous = best.score < resolveThreshold || (second != null && best.score - second.score < ambiguityMargin && second.score >= resolveThreshold)

        return if (ambiguous) {
            EntityResolutionResult(mention, EntityResolutionStatus.AMBIGUOUS, candidates = candidates)
        } else {
            EntityResolutionResult(mention, EntityResolutionStatus.RESOLVED, entityId = best.entity.id, candidates = candidates)
        }
    }

    private fun score(entity: Entity, normalizedMention: String, mention: SemanticMention): EntityResolutionCandidate? {
        val canonical = normalize(entity.name)
        if (normalizedMention == canonical) return EntityResolutionCandidate(entity, 1.0, EntityResolutionReason.EXACT_CANONICAL_NAME)

        val aliases = registry.aliasesFor(entity.id)
        val exactAlias = aliases.firstOrNull { normalize(it.value) == normalizedMention }
        if (exactAlias != null) return EntityResolutionCandidate(entity, (0.98 * exactAlias.confidence).coerceIn(0.0, 1.0), EntityResolutionReason.EXACT_ALIAS)

        if (normalizedMention == canonical.replace(" ", "")) return EntityResolutionCandidate(entity, 0.92, EntityResolutionReason.NORMALIZED_CANONICAL_NAME)
        val normalizedAlias = aliases.firstOrNull { normalize(it.value).replace(" ", "") == normalizedMention.replace(" ", "") }
        if (normalizedAlias != null) return EntityResolutionCandidate(entity, (0.90 * normalizedAlias.confidence).coerceIn(0.0, 1.0), EntityResolutionReason.NORMALIZED_ALIAS)

        val hint = mention.typeHint?.trim()?.uppercase()
        if (hint != null && hint == entity.type.name && normalizedMention.contains(canonical)) {
            return EntityResolutionCandidate(entity, 0.86, EntityResolutionReason.TYPE_COMPATIBLE_NAME)
        }
        return null
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
}
