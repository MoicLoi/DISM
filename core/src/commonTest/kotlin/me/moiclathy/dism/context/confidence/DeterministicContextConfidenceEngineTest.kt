package me.moiclathy.dism.context.confidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.source.ContextSourceId
import me.moiclathy.dism.context.source.ContextSourceType

class DeterministicContextConfidenceEngineTest {

    private val engine =
        DeterministicContextConfidenceEngine()

    private val start =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val afterHalfLife =
        Instant.parse(
            "2026-09-16T14:00:00Z"
        )

    private val policy =
        ContextConfidencePolicy(
            halfLifeSeconds = 6.0 * 60.0 * 60.0,
        )

    @Test
    fun confidenceDecaysByHalfLife() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.80,
                lastUpdatedAt = start,
            )

        val result =
            engine.decay(
                confidence = initial,
                now = afterHalfLife,
                policy = policy,
            )

        assertEquals(
            0.40,
            result.confidence.value,
            0.000001,
        )
    }

    @Test
    fun confidenceDoesNotIncreaseWithoutEvidence() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.80,
                lastUpdatedAt = start,
            )

        val result =
            engine.decay(
                confidence = initial,
                now = start,
                policy = policy,
            )

        assertEquals(
            0.80,
            result.confidence.value,
            0.000001,
        )
    }

    @Test
    fun strongEvidenceIncreasesConfidence() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.40,
                lastUpdatedAt = start,
            )

        val evidence =
            ContextConfidenceEvidence(
                sourceId =
                    ContextSourceId("schedule"),
                sourceType =
                    ContextSourceType.SCHEDULE,
                strength = 1.0,
                observedAt = start,
            )

        val result =
            engine.applyEvidence(
                confidence = initial,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        assertEquals(
            0.90,
            result.confidence.value,
            0.000001,
        )

        assertEquals(
            1,
            result.acceptedEvidence.size,
        )
    }

    @Test
    fun evidenceCannotExceedMaximum() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.90,
                lastUpdatedAt = start,
            )

        val evidence =
            ContextConfidenceEvidence(
                sourceId =
                    ContextSourceId("user"),
                sourceType =
                    ContextSourceType.USER_INPUT,
                strength = 1.0,
                observedAt = start,
            )

        val result =
            engine.applyEvidence(
                confidence = initial,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        assertEquals(
            1.0,
            result.confidence.value,
            0.000001,
        )
    }

    @Test
    fun expiredEvidenceIsRejected() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.40,
                lastUpdatedAt = start,
            )

        val evidence =
            ContextConfidenceEvidence(
                sourceId =
                    ContextSourceId("schedule"),
                sourceType =
                    ContextSourceType.SCHEDULE,
                strength = 1.0,
                observedAt = start,
                expiresAt =
                    Instant.parse(
                        "2026-09-16T07:00:00Z"
                    ),
            )

        val result =
            engine.applyEvidence(
                confidence = initial,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        assertEquals(
            0.40,
            result.confidence.value,
            0.000001,
        )

        assertEquals(
            1,
            result.rejectedEvidence.size,
        )
    }

    @Test
    fun repeatedEvidenceProducesMonotonicIncrease() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.20,
                lastUpdatedAt = start,
            )

        val evidence =
            ContextConfidenceEvidence(
                sourceId =
                    ContextSourceId("observation"),
                sourceType =
                    ContextSourceType.OBSERVATION,
                strength = 0.50,
                observedAt = start,
            )

        val first =
            engine.applyEvidence(
                confidence = initial,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        val second =
            engine.applyEvidence(
                confidence = first.confidence,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        assertTrue(
            second.confidence.value >
                first.confidence.value
        )
    }

    @Test
    fun confidenceEventuallyReachesZeroAfterDecay() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.80,
                lastUpdatedAt = start,
            )

        val farFuture =
            Instant.parse(
                "2026-10-16T08:00:00Z"
            )

        val result =
            engine.decay(
                confidence = initial,
                now = farFuture,
                policy = policy,
            )

        assertEquals(
            0.0,
            result.confidence.value,
            0.000001,
        )
    }

    @Test
    fun updateIsDeterministic() {

        val initial =
            ContextConfidence(
                mode = ContextMode.IN_CLASS,
                value = 0.70,
                lastUpdatedAt = start,
            )

        val evidence =
            ContextConfidenceEvidence(
                sourceId =
                    ContextSourceId("user"),
                sourceType =
                    ContextSourceType.USER_INPUT,
                strength = 0.80,
                observedAt = start,
            )

        val first =
            engine.applyEvidence(
                confidence = initial,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        val second =
            engine.applyEvidence(
                confidence = initial,
                evidence = evidence,
                now = start,
                policy = policy,
            )

        assertEquals(
            first,
            second,
        )
    }
}
