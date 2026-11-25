package me.moiclathy.dism.context.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextClaim
import me.moiclathy.dism.context.model.ContextId
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.model.ContextSource
import me.moiclathy.dism.context.source.ContextSourceId
import me.moiclathy.dism.context.source.ContextSourceResult
import me.moiclathy.dism.context.source.ContextSourceType

class DeterministicContextResolverTest {

    private val resolver =
        DeterministicContextResolver()

    private val policy =
        ContextResolutionPolicy()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    @Test
    fun singleValidClaimResolves() {

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "schedule",
                    type = ContextSourceType.SCHEDULE,
                    claim = claim(
                        mode = ContextMode.IN_CLASS,
                        confidence = 0.90,
                    ),
                )
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            ContextResolutionStatus.RESOLVED,
            result.status,
        )

        assertEquals(
            ContextMode.IN_CLASS,
            result.resolvedMode,
        )

        assertEquals(
            1,
            result.candidates.size,
        )
    }

    @Test
    fun expiredClaimIsIgnored() {

        val expiredClaim = ContextClaim(
            id = "expired-claim",
            contextId = ContextId("situation"),
            mode = ContextMode.IN_CLASS,
            confidence = 0.99,
            source = ContextSource.SCHEDULE,
            observedAt = Instant.parse(
                "2026-09-16T06:00:00Z"
            ),
            expiresAt = Instant.parse(
                "2026-09-16T07:00:00Z"
            ),
        )

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "schedule",
                    type = ContextSourceType.SCHEDULE,
                    claim = expiredClaim,
                )
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            ContextResolutionStatus.UNRESOLVED,
            result.status,
        )

        assertNull(
            result.resolvedMode
        )

        assertEquals(
            1,
            result.rejectedClaimCount,
        )
    }

    @Test
    fun lowConfidenceClaimIsIgnored() {

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "derived",
                    type = ContextSourceType.DERIVED,
                    claim = claim(
                        mode = ContextMode.RESTING,
                        confidence = 0.10,
                    ),
                )
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            ContextResolutionStatus.UNRESOLVED,
            result.status,
        )
    }

    @Test
    fun strongerContextWins() {

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "schedule",
                    type = ContextSourceType.SCHEDULE,
                    claim = claim(
                        mode = ContextMode.IN_CLASS,
                        confidence = 0.95,
                    ),
                ),
                sourceResult(
                    id = "derived",
                    type = ContextSourceType.DERIVED,
                    claim = claim(
                        mode = ContextMode.RESTING,
                        confidence = 0.50,
                    ),
                )
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            ContextResolutionStatus.RESOLVED,
            result.status,
        )

        assertEquals(
            ContextMode.IN_CLASS,
            result.resolvedMode,
        )
    }

    @Test
    fun closeScoresProduceAmbiguousResult() {

        val customPolicy =
            ContextResolutionPolicy(
                ambiguityMargin = 0.10,
            )

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "user",
                    type = ContextSourceType.USER_INPUT,
                    claim = claim(
                        mode = ContextMode.FOCUSED,
                        confidence = 0.90,
                    ),
                ),
                sourceResult(
                    id = "observation",
                    type = ContextSourceType.OBSERVATION,
                    claim = claim(
                        mode = ContextMode.IN_CLASS,
                        confidence = 0.94,
                    ),
                )
            ),
            now = now,
            policy = customPolicy,
        )

        assertEquals(
            ContextResolutionStatus.AMBIGUOUS,
            result.status,
        )

        assertNull(
            result.resolvedMode
        )
    }

    @Test
    fun sameModeFromMultipleSourcesIsAggregated() {

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "schedule",
                    type = ContextSourceType.SCHEDULE,
                    claim = claim(
                        mode = ContextMode.IN_CLASS,
                        confidence = 0.80,
                    ),
                ),
                sourceResult(
                    id = "observation",
                    type = ContextSourceType.OBSERVATION,
                    claim = claim(
                        mode = ContextMode.IN_CLASS,
                        confidence = 0.82,
                    ),
                )
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            ContextResolutionStatus.RESOLVED,
            result.status,
        )

        assertEquals(
            ContextMode.IN_CLASS,
            result.resolvedMode,
        )

        val candidate =
            result.bestCandidate!!

        assertEquals(
            2,
            candidate.supportingSourceCount,
        )

        assertTrue(
            ContextResolutionReason.MULTI_SOURCE_SUPPORT
                in candidate.reasons
        )
    }

    @Test
    fun userInputCanOverrideWeakerScheduleClaim() {

        val result = resolver.resolve(
            sources = listOf(
                sourceResult(
                    id = "schedule",
                    type = ContextSourceType.SCHEDULE,
                    claim = claim(
                        mode = ContextMode.IN_CLASS,
                        confidence = 0.65,
                    ),
                ),
                sourceResult(
                    id = "user",
                    type = ContextSourceType.USER_INPUT,
                    claim = claim(
                        mode = ContextMode.RESTING,
                        confidence = 0.95,
                    ),
                )
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            ContextResolutionStatus.RESOLVED,
            result.status,
        )

        assertEquals(
            ContextMode.RESTING,
            result.resolvedMode,
        )
    }

    @Test
    fun resultIsDeterministicRegardlessOfInputOrder() {

        val first =
            sourceResult(
                id = "schedule",
                type = ContextSourceType.SCHEDULE,
                claim = claim(
                    mode = ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),
            )

        val second =
            sourceResult(
                id = "derived",
                type = ContextSourceType.DERIVED,
                claim = claim(
                    mode = ContextMode.RESTING,
                    confidence = 0.40,
                ),
            )

        val resultA = resolver.resolve(
            sources = listOf(
                first,
                second,
            ),
            now = now,
            policy = policy,
        )

        val resultB = resolver.resolve(
            sources = listOf(
                second,
                first,
            ),
            now = now,
            policy = policy,
        )

        assertEquals(
            resultA.status,
            resultB.status,
        )

        assertEquals(
            resultA.resolvedMode,
            resultB.resolvedMode,
        )

        assertEquals(
            resultA.candidates.map {
                it.mode
            },
            resultB.candidates.map {
                it.mode
            },
        )

        assertEquals(
            resultA.candidates.map {
                it.score
            },
            resultB.candidates.map {
                it.score
            },
        )
    }

    private fun claim(
        mode: ContextMode,
        confidence: Double,
    ): ContextClaim {

        return ContextClaim(
            id = "c-${mode.name}",
            contextId = ContextId("situation"),
            mode = mode,
            confidence = confidence,
            source = ContextSource.SYSTEM,
            observedAt = now,
        )
    }

    private fun sourceResult(
        id: String,
        type: ContextSourceType,
        claim: ContextClaim,
    ): ContextSourceResult {

        return ContextSourceResult(
            sourceId = ContextSourceId(id),
            sourceType = type,
            claims = listOf(claim),
        )
    }
}
