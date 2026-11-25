package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.mapper.EvidenceMapper
import kotlin.uuid.Uuid

/**
 * SQLDelight 实现的 EvidenceRepository。
 */
class SqlDelightEvidenceRepository(
    database: DismDatabase,
) : EvidenceRepository {

    private val queries =
        database.evidenceQueries

    override fun save(
        evidence: Evidence,
    ) {
        queries.insertOrReplace(
            id = evidence.id.toString(),
            type = evidence.type.name,
            source = evidence.source,
            observed_at =
                evidence.observedAt.toString(),
            confidence =
                evidence.confidence,
        )
    }

    override fun find(
        id: Uuid,
    ): Evidence? {
        return queries
            .selectById(
                id.toString()
            )
            .executeAsOneOrNull()
            ?.let(
                EvidenceMapper::toDomain
            )
    }

    override fun all(): List<Evidence> {
        return queries
            .selectAll()
            .executeAsList()
            .map(
                EvidenceMapper::toDomain
            )
    }
}
