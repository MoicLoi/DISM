package me.moiclathy.dism.persistence.db.mapper

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.Evidence as DbEvidence

object EvidenceMapper {

    @OptIn(ExperimentalUuidApi::class)
    fun toDomain(
        row: DbEvidence,
    ): Evidence {
        return Evidence(
            id = Uuid.parse(
                row.id
            ),
            type = EvidenceType.valueOf(
                row.type
            ),
            source = row.source,
            observedAt = Instant.parse(
                row.observed_at
            ),
            confidence = row.confidence,
        )
    }
}
