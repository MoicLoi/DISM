package me.moiclathy.dism.persistence.db.mapper

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.persistence.db.Relation_state as DbRelationState
import me.moiclathy.dism.persistence.db.Relation_support as DbRelationSupport

/**
 * 一个 SQL row 不能直接对应一个 Domain object：
 * RelationState 由 relation_state + relation_support 两表恢复。
 *
 * SQL rows
 *     ↓ grouping by (RelationKey + validFrom)
 *     ↓ RelationStateMapper
 *     ↓ RelationState
 */
object RelationStateMapper {

    @OptIn(ExperimentalUuidApi::class)
    fun toDomain(
        states: List<DbRelationState>,
        supports: List<DbRelationSupport>,
    ): List<RelationState> {

        val supportsByState =
            supports.groupBy {
                StateKey(
                    subjectId = it.subject_id,
                    predicate = it.predicate,
                    objectId = it.object_id,
                    validFrom = it.valid_from,
                )
            }

        return states.map { row ->

            val key =
                StateKey(
                    subjectId = row.subject_id,
                    predicate = row.predicate,
                    objectId = row.object_id,
                    validFrom = row.valid_from,
                )

            val supportRows =
                supportsByState[key].orEmpty()

            RelationState(
                key = RelationKey(
                    subjectId = Uuid.parse(
                        row.subject_id
                    ),
                    predicate = Predicate(
                        row.predicate
                    ),
                    objectId = Uuid.parse(
                        row.object_id
                    ),
                ),
                confidence = row.confidence,
                supports = supportRows
                    .map { support ->
                        RelationSupport(
                            evidenceId = Uuid.parse(
                                support.evidence_id
                            ),
                            semanticConfidence =
                                support.semantic_confidence,
                        )
                    }
                    .toSet(),
                validFrom = Instant.parse(
                    row.valid_from
                ),
                validUntil = row.valid_until
                    ?.let(Instant::parse),
                status = RelationStatus.valueOf(
                    row.status
                ),
            )
        }
    }

    /**
     * 不能用 RelationKey 当 grouping key：
     * 同一关系历史上可多次成立（Dorm 07:00 / Classroom 10:00 / Dorm 18:00）。
     * 第一版 identity = RelationKey + validFrom，与 Timeline diff 一致。
     */
    private data class StateKey(
        val subjectId: String,
        val predicate: String,
        val objectId: String,
        val validFrom: String,
    )
}
