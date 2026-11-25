package me.moiclathy.dism.persistence.repository

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.mapper.TransitionRecordMapper
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.time.Instant

/**
 * SQLDelight 实现的 TransitionLogRepository。
 *
 * transition_record 存 audit 结构（type + 两套 key 列），
 * transition_evidence 存 evidenceIds 关联——不存 JSON blob。
 */
class SqlDelightTransitionLogRepository(
    private val database: DismDatabase,
) : TransitionLogRepository {

    private val recordQueries =
        database.transitionRecordQueries

    private val evidenceQueries =
        database.transitionEvidenceQueries

    @OptIn(ExperimentalUuidApi::class)
    override fun append(
        record: StateTransitionRecord,
    ) {
        database.transaction {

            val columns =
                toColumns(record.transition)

            recordQueries.insertRecord(
                id = record.id.toString(),
                type = columns.type,
                subject_id = columns.subjectId,
                predicate = columns.predicate,
                object_id = columns.objectId,
                effective_at =
                    record.effectiveAt.toString(),
                semantic_confidence =
                    columns.semanticConfidence,
                old_subject_id =
                    columns.oldSubjectId,
                old_predicate =
                    columns.oldPredicate,
                old_object_id =
                    columns.oldObjectId,
            )

            record.transition.evidenceIds
                .forEach { evidenceId ->
                    evidenceQueries.insert(
                        transition_id =
                            record.id.toString(),
                        evidence_id =
                            evidenceId.toString(),
                    )
                }
        }
    }

    override fun all(): List<StateTransitionRecord> {
        val rows =
            recordQueries
                .selectAll()
                .executeAsList()

        return toDomainRows(rows)
    }

    override fun at(
        instant: Instant,
    ): List<StateTransitionRecord> {
        val rows =
            recordQueries
                .selectAt(
                    instant.toString()
                )
                .executeAsList()

        return toDomainRows(rows)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun toDomainRows(
        rows: List<me.moiclathy.dism.persistence.db.Transition_record>,
    ): List<StateTransitionRecord> {
        return rows.map { row ->
            TransitionRecordMapper.toDomain(
                row = row,
                evidenceIds = evidenceQueries
                    .selectForTransition(
                        row.id
                    )
                    .executeAsList()
                    .map(Uuid::parse)
                    .toSet(),
            )
        }
    }

    private fun toColumns(
        transition: StateTransition,
    ): TransitionColumns {
        return when (transition) {

            is StateTransition.AssertRelation ->
                TransitionColumns(
                    type = "AssertRelation",
                    subjectId =
                        transition.key.subjectId.toString(),
                    predicate =
                        transition.key.predicate.value,
                    objectId =
                        transition.key.objectId.toString(),
                    semanticConfidence =
                        transition.semanticConfidence,
                )

            is StateTransition.SupersedeRelation ->
                TransitionColumns(
                    type = "SupersedeRelation",
                    subjectId =
                        transition.newKey.subjectId.toString(),
                    predicate =
                        transition.newKey.predicate.value,
                    objectId =
                        transition.newKey.objectId.toString(),
                    semanticConfidence =
                        transition.semanticConfidence,
                    oldSubjectId =
                        transition.oldKey.subjectId.toString(),
                    oldPredicate =
                        transition.oldKey.predicate.value,
                    oldObjectId =
                        transition.oldKey.objectId.toString(),
                )

            is StateTransition.InvalidateRelation ->
                TransitionColumns(
                    type = "InvalidateRelation",
                    subjectId =
                        transition.key.subjectId.toString(),
                    predicate =
                        transition.key.predicate.value,
                    objectId =
                        transition.key.objectId.toString(),
                )
        }
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
