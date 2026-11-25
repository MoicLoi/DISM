package me.moiclathy.dism.nl.query.execution

import me.moiclathy.dism.nl.query.SemanticStateQuery

/**
 * Validates a resolved query without executing it against the state store.
 * Keeping this separate prevents the NL layer from silently mutating or inventing state.
 */
class DeterministicSemanticStateQueryExecutor : SemanticStateQueryExecutor {
    override fun prepare(query: SemanticStateQuery): SemanticStateQueryExecution {
        val executable = query.subjectEntityId != null
        return SemanticStateQueryExecution(
            query = query,
            executable = executable,
            reason = if (executable) "resolved_query_ready" else "query_entity_unresolved",
        )
    }
}
