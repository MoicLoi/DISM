package me.moiclathy.dism.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository

class EvidenceRegistryTest {

    @Test
    fun registeredEvidence_shouldBeRetrievable() {
        val registry = InMemoryEvidenceRepository()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-08T08:00:00Z"),
            confidence = 1.0,
        )

        registry.register(evidence)

        assertTrue(registry.contains(evidence.id))
        assertEquals(evidence, registry.find(evidence.id))
    }

    @Test
    fun missingEvidence_shouldReturnNull() {
        val registry = InMemoryEvidenceRepository()

        assertNull(registry.find(Uuid.random()))
    }

    @Test
    fun unknownEvidence_shouldFailRequire() {
        val registry = InMemoryEvidenceRepository()

        assertFailsWith<IllegalArgumentException> {
            registry.require(Uuid.random())
        }
    }
}
