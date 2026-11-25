package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.evidence.Evidence
import kotlin.uuid.Uuid

/**
 * 第一版内存实现，主要给 Core 测试使用。
 *
 * 同时保留 EvidenceRegistry 时期的兼容 API：
 * register / registerAll / require，
 * 避免既有语义测试推倒重写。
 */
class InMemoryEvidenceRepository :
    EvidenceRepository {

    private val evidence =
        mutableMapOf<Uuid, Evidence>()

    override fun save(
        evidence: Evidence,
    ) {
        this.evidence[evidence.id] = evidence
    }

    fun register(
        evidence: Evidence,
    ) {
        save(evidence)
    }

    fun registerAll(
        items: Iterable<Evidence>,
    ) {
        items.forEach(::register)
    }

    override fun find(
        id: Uuid,
    ): Evidence? {
        return evidence[id]
    }

    fun require(
        id: Uuid,
    ): Evidence {
        return requireNotNull(find(id)) {
            "Evidence not found: $id"
        }
    }

    override fun all(): List<Evidence> {
        return evidence.values.toList()
    }

    override fun contains(
        id: Uuid,
    ): Boolean {
        return id in evidence
    }
}
