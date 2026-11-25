package me.moiclathy.dism.persistence.db.mapper

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import me.moiclathy.dism.persistence.db.Transition_record as DbTransitionRecord

/**
 * transition_record 不存 JSON blob：
 * type 决定变体，subject/predicate/object + old_* 列恢复 RelationKey，
 * evidenceIds 由 transition_evidence 表恢复。
 */
object TransitionRecordMapper {

    @OptIn(ExperimentalUuidApi::class)
    fun toDomain(
        row: DbTransitionRecord,
        evidenceIds: Set<Uuid>,
    ): StateTransitionRecord {

        val effectiveAt =
            Instant.parse(row.effective_at)

        val transition =
            when (row.type) {

                "AssertRelation" ->
                    StateTransition.AssertRelation(
                        key = relationKey(
                            subjectId = row.subject_id,
                            predicate = row.predicate,
                            objectId = row.object_id,
                        ),
                        semanticConfidence =
                            requireNotNull(
                                row.semantic_confidence
                            ) {
                                "AssertRelation is missing semantic_confidence: ${row.id}"
                            },
                        effectiveAt = effectiveAt,
                        evidenceIds = evidenceIds,
                    )

                "SupersedeRelation" ->
                    StateTransition.SupersedeRelation(
                        oldKey = relationKey(
                            subjectId = requireNotNull(
                                row.old_subject_id
                            ) {
                                "SupersedeRelation is missing old_subject_id: ${row.id}"
                            },
                            predicate = requireNotNull(
                                row.old_predicate
                            ) {
                                "SupersedeRelation is missing old_predicate: ${row.id}"
                            },
                            objectId = requireNotNull(
                                row.old_object_id
                            ) {
                                "SupersedeRelation is missing old_object_id: ${row.id}"
                            },
                        ),
                        newKey = relationKey(
                            subjectId = row.subject_id,
                            predicate = row.predicate,
                            objectId = row.object_id,
                        ),
                        semanticConfidence =
                            requireNotNull(
                                row.semantic_confidence
                            ) {
                                "SupersedeRelation is missing semantic_confidence: ${row.id}"
                            },
                        effectiveAt = effectiveAt,
                        evidenceIds = evidenceIds,
                    )

                "InvalidateRelation" ->
                    StateTransition.InvalidateRelation(
                        key = relationKey(
                            subjectId = row.subject_id,
                            predicate = row.predicate,
                            objectId = row.object_id,
                        ),
                        effectiveAt = effectiveAt,
                        evidenceIds = evidenceIds,
                    )

                else ->
                    error(
                        "Unknown transition type: ${row.type}"
                    )
            }

        return StateTransitionRecord(
            id = Uuid.parse(row.id),
            effectiveAt = effectiveAt,
            transition = transition,
        )
    }

    private fun relationKey(
        subjectId: String,
        predicate: String,
        objectId: String,
    ): RelationKey {
        return RelationKey(
            subjectId = Uuid.parse(subjectId),
            predicate = Predicate(predicate),
            objectId = Uuid.parse(objectId),
        )
    }
}
