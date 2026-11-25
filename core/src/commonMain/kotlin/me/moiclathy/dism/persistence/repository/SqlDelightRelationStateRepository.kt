package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.mapper.RelationStateMapper
import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Instant

/**
 * SQLDelight 实现的 RelationStateRepository。
 *
 * RelationState
 *     ├── relation_state 表
 *     └── relation_support 表
 *              │
 *              └── evidence
 *
 * 状态与"为什么相信这个状态"分离：
 * evidenceIds 不是 relation_state 表里的字符串字段，
 * 而是从 relation_support 关联表恢复。
 */
class SqlDelightRelationStateRepository(
    private val database: DismDatabase,
) : RelationStateRepository {

    private val stateQueries =
        database.relationStateQueries

    private val supportQueries =
        database.relationSupportQueries

    override fun loadAll(): List<RelationState> {
        return RelationStateMapper.toDomain(
            states = stateQueries
                .selectAll()
                .executeAsList(),
            supports = supportQueries
                .selectAll()
                .executeAsList(),
        )
    }

    /**
     * 全量原子替换：
     * 先删 support、再删 state（FK 顺序），
     * 再插入全部 states + supports。
     *
     * BEGIN TRANSACTION
     *   DELETE support
     *   DELETE state
     *   INSERT states + supports
     * COMMIT
     *
     * 中途失败 → ROLLBACK，不会出现"state 有了 support 没了"。
     * 当前 MVP 数据量下全量替换足够且最容易保证一致性；
     * 以后测出性能瓶颈再升级增量持久化。
     */
    override fun replaceAll(
        states: List<RelationState>,
    ) {
        database.transaction {

            supportQueries.deleteAll()
            stateQueries.deleteAll()

            states.forEach { state ->

                stateQueries.insert(
                    subject_id =
                        state.key.subjectId.toString(),
                    predicate =
                        state.key.predicate.value,
                    object_id =
                        state.key.objectId.toString(),
                    confidence =
                        state.confidence,
                    valid_from =
                        state.validFrom.toString(),
                    valid_until =
                        state.validUntil?.toString(),
                    status =
                        state.status.name,
                )

                state.supports.forEach { support ->

                    supportQueries.insert(
                        subject_id =
                            state.key.subjectId.toString(),
                        predicate =
                            state.key.predicate.value,
                        object_id =
                            state.key.objectId.toString(),
                        valid_from =
                            state.validFrom.toString(),
                        evidence_id =
                            support.evidenceId.toString(),
                        semantic_confidence =
                            support.semanticConfidence,
                    )
                }
            }
        }
    }

    override fun queryActiveAt(
        instant: Instant,
    ): List<RelationState> {

        val timestamp = instant.toString()

        val stateRows =
            stateQueries
                .selectActiveAt(
                    timestamp,
                    timestamp,
                )
                .executeAsList()

        return RelationStateMapper.toDomain(
            states = stateRows,
            supports = supportQueries
                .selectAll()
                .executeAsList(),
        )
    }
}
