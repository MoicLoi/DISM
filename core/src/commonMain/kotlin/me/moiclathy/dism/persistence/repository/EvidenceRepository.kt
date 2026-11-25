package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.evidence.Evidence
import kotlin.uuid.Uuid

/**
 * Evidence 持久化边界。
 */
interface EvidenceRepository {

    fun save(
        evidence: Evidence,
    )

    fun saveAll(
        items: Iterable<Evidence>,
    ) {
        items.forEach(::save)
    }

    fun find(
        id: Uuid,
    ): Evidence?

    fun all(): List<Evidence>

    fun contains(
        id: Uuid,
    ): Boolean {
        return find(id) != null
    }
}
