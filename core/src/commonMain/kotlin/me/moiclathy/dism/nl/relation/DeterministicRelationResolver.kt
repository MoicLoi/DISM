package me.moiclathy.dism.nl.relation

import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates

/**
 * Resolves relation words to the open Predicate vocabulary without mutating semantic state.
 */
class DeterministicRelationResolver : RelationResolver {
    private val aliases = mapOf(
        "located_at" to Predicates.LOCATED_AT,
        "location" to Predicates.LOCATED_AT,
        "locate" to Predicates.LOCATED_AT,
        "位于" to Predicates.LOCATED_AT,
        "在" to Predicates.LOCATED_AT,
        "放在" to Predicates.LOCATED_AT,
        "放置在" to Predicates.LOCATED_AT,
        "inside" to Predicates.INSIDE,
        "内部" to Predicates.INSIDE,
        "里面" to Predicates.INSIDE,
        "在里面" to Predicates.INSIDE,
        "carried_by" to Predicates.CARRIED_BY,
        "carried" to Predicates.CARRIED_BY,
        "携带" to Predicates.CARRIED_BY,
        "拿着" to Predicates.CARRIED_BY,
        "requires" to Predicates.REQUIRES,
        "require" to Predicates.REQUIRES,
        "需要" to Predicates.REQUIRES,
        "需要带" to Predicates.REQUIRES,
    )

    override fun resolve(mention: RelationMention): RelationResolutionResult {
        val raw = mention.predicateSurface.trim()
        if (raw.isBlank()) return RelationResolutionResult(mention, null, 0.0)
        val normalized = normalize(raw)
        val exact = aliases[raw]
        if (exact != null) return result(mention, exact, 1.0, PredicateResolutionReason.EXACT_CANONICAL)
        val canonical = aliases[normalized]
        if (canonical != null) return result(mention, canonical, 0.95, PredicateResolutionReason.NORMALIZED_CANONICAL)
        return RelationResolutionResult(mention, null, 0.0)
    }

    private fun result(m: RelationMention, p: Predicate, score: Double, reason: PredicateResolutionReason) =
        RelationResolutionResult(m, p, score * m.confidence, reason)

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
}
