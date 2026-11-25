package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * SQLDelight 的原子提交器。
 *
 * 一次 observe 可能产生多个 StateTransitionRecord，以及大量 timeline changes。
 * Dynamic State 与 Transition Log 必须共享同一个 SQLite transaction。
 */
class SqlDelightStateCommitRepository(
    private val database: DismDatabase,
) : StateCommitRepository {

    private val stateQueries = database.relationStateQueries
    private val supportQueries = database.relationSupportQueries
    private val recordQueries = database.transitionRecordQueries
    private val transitionEvidenceQueries = database.transitionEvidenceQueries

    override fun commit(
        states: List<RelationState>,
        transitions: List<StateTransitionRecord>,
    ) {
        database.transaction {
            replaceStates(states)
            appendTransitions(transitions)
        }
    }

    private fun replaceStates(
        states: List<RelationState>,
    ) {
        supportQueries.deleteAll()
        stateQueries.deleteAll()

        states.forEach { state ->
            stateQueries.insert(
                subject_id = state.key.subjectId.toString(),
                predicate = state.key.predicate.value,
                object_id = state.key.objectId.toString(),
                confidence = state.confidence,
                valid_from = state.validFrom.toString(),
                valid_until = state.validUntil?.toString(),
                status = state.status.name,
            )

            state.supports.forEach { support ->
                supportQueries.insert(
                    subject_id = state.key.subjectId.toString(),
                    predicate = state.key.predicate.value,
                    object_id = state.key.objectId.toString(),
                    valid_from = state.validFrom.toString(),
                    evidence_id = support.evidenceId.toString(),
                    semantic_confidence = support.semanticConfidence,
                )
            }
        }
    }

    private fun appendTransitions(
        transitions: List<StateTransitionRecord>,
    ) {
        transitions.forEach { record ->
            val columns = toColumns(record.transition)

            recordQueries.insertRecord(
                id = record.id.toString(),
                type = columns.type,
                subject_id = columns.subjectId,
                predicate = columns.predicate,
                object_id = columns.objectId,
                effective_at = record.effectiveAt.toString(),
                semantic_confidence = columns.semanticConfidence,
                old_subject_id = columns.oldSubjectId,
                old_predicate = columns.oldPredicate,
                old_object_id = columns.oldObjectId,
            )

            record.transition.evidenceIds.forEach { evidenceId ->
                transitionEvidenceQueries.insert(
                    transition_id = record.id.toString(),
                    evidence_id = evidenceId.toString(),
                )
            }
        }
    }

    private fun toColumns(
        transition: StateTransition,
    ): TransitionColumns = when (transition) {
        is StateTransition.AssertRelation ->
            TransitionColumns(
                type = "AssertRelation",
                subjectId = transition.key.subjectId.toString(),
                predicate = transition.key.predicate.value,
                objectId = transition.key.objectId.toString(),
                semanticConfidence = transition.semanticConfidence,
            )

        is StateTransition.SupersedeRelation ->
            TransitionColumns(
                type = "SupersedeRelation",
                subjectId = transition.newKey.subjectId.toString(),
                predicate = transition.newKey.predicate.value,
                objectId = transition.newKey.objectId.toString(),
                semanticConfidence = transition.semanticConfidence,
                oldSubjectId = transition.oldKey.subjectId.toString(),
                oldPredicate = transition.oldKey.predicate.value,
                oldObjectId = transition.oldKey.objectId.toString(),
            )

        is StateTransition.InvalidateRelation ->
            TransitionColumns(
                type = "InvalidateRelation",
                subjectId = transition.key.subjectId.toString(),
                predicate = transition.key.predicate.value,
                objectId = transition.key.objectId.toString(),
            )
    }

    private data class TransitionColumns(
        val type: String,
        val subjectId: String,
        val predicate: String,
        val objectId: String,
        val semanticConfidence: Double? = null,
        val oldSubjectId: String? = null,
        val oldPredicate: String? = null,
        val oldObjectId: String? = null,
    )
}
