package me.moiclathy.dism.nl.query

import me.moiclathy.dism.nl.SemanticIntent
import me.moiclathy.dism.nl.SemanticIntentType

/** Conservative compiler. Entity IDs are supplied by a later resolution stage. */
class DeterministicSemanticQueryCompiler : SemanticQueryCompiler {
    override fun compile(intent: SemanticIntent): List<SemanticStateQueryCandidate> {
        if (intent.type != SemanticIntentType.QUERY_STATE) return emptyList()
        val mentions = intent.mentions
        val subject = mentions.firstOrNull()
        val text = mentions.joinToString(" ") { it.normalized }
        val kind = when {
            text.contains("需要") || text.contains("带什么") -> SemanticStateQueryKind.REQUIREMENTS
            text.contains("哪里") || text.contains("在哪") -> SemanticStateQueryKind.LOCATION
            else -> SemanticStateQueryKind.STATE
        }
        return listOf(
            SemanticStateQueryCandidate(
                query = SemanticStateQuery(
                    queryKind = kind,
                    confidence = intent.confidence,
                ),
                unresolvedMentions = listOfNotNull(subject?.normalized),
                rationale = "QUERY_STATE intent compiled without mutating semantic state",
            )
        )
    }
}
