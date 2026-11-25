package me.moiclathy.dism.semantic.confidence

import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RelationConfidenceAggregatorTest {

    private val aggregator =
        RelationConfidenceAggregator()

    @Test
    fun singleSupport_shouldCombineEvidenceAndSemanticConfidence() {
        val registry =
            InMemoryEvidenceRepository()

        val evidence =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt =
                    Instant.parse("2026-09-08T08:00:00Z"),
                confidence = 0.8,
            )

        registry.register(evidence)

        val result =
            aggregator.aggregate(
                supports =
                    setOf(
                        RelationSupport(
                            evidenceId = evidence.id,
                            semanticConfidence = 0.75,
                        )
                    ),
                evidenceRegistry = registry,
            )

        // 0.8 × 0.75 = 0.60
        assertEquals(
            0.6,
            result,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun independentSupports_shouldIncreaseConfidence() {
        val registry =
            InMemoryEvidenceRepository()

        val time =
            Instant.parse("2026-09-08T08:00:00Z")

        val first =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "source-a",
                observedAt = time,
                confidence = 0.8,
            )

        val second =
            Evidence(
                type = EvidenceType.SCHEDULE_IMPORT,
                source = "source-b",
                observedAt = time,
                confidence = 0.7,
            )

        registry.registerAll(
            listOf(first, second)
        )

        val result =
            aggregator.aggregate(
                supports =
                    setOf(
                        RelationSupport(
                            evidenceId = first.id,
                            semanticConfidence = 1.0,
                        ),
                        RelationSupport(
                            evidenceId = second.id,
                            semanticConfidence = 1.0,
                        ),
                    ),
                evidenceRegistry = registry,
            )

        // 1 - (1 - 0.8)(1 - 0.7) = 0.94
        assertEquals(
            0.94,
            result,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun missingEvidence_shouldBeExcludedFromAggregation() {
        val registry = InMemoryEvidenceRepository()

        val result = aggregator.aggregate(
            supports = setOf(
                RelationSupport(
                    evidenceId = kotlin.uuid.Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            evidenceRegistry = registry,
        )

        assertEquals(0.0, result)
    }

    @Test
    fun emptySupports_shouldProduceZeroConfidence() {
        val result = aggregator.aggregate(
            supports = emptySet(),
            evidenceRegistry = InMemoryEvidenceRepository(),
        )

        assertEquals(0.0, result)
    }
}
