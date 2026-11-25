package me.moiclathy.dism.context.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextClaim
import me.moiclathy.dism.context.model.ContextId
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.model.ContextSource

class ContextSourceTest {

    private val now =
        Instant.parse("2026-09-16T08:00:00Z")

    @Test
    fun staticSourceReturnsConfiguredClaims() {
        val claim = ContextClaim(
            id = "test-claim",
            contextId = ContextId("situation"),
            mode = ContextMode.IN_CLASS,
            confidence = 0.95,
            source = ContextSource.SYSTEM,
            observedAt = now,
        )

        val source = StaticContextSource(
            id = ContextSourceId("test"),
            type = ContextSourceType.OBSERVATION,
            claims = listOf(claim),
        )

        val result = source.collect(
            ContextSourceInput(
                now = now,
            )
        )

        assertEquals(
            ContextSourceId("test"),
            result.sourceId,
        )

        assertEquals(
            ContextSourceType.OBSERVATION,
            result.sourceType,
        )

        assertEquals(
            listOf(claim),
            result.claims,
        )
    }

    @Test
    fun registryKeepsSourcesByStableId() {
        val source = StaticContextSource(
            id = ContextSourceId("schedule"),
            type = ContextSourceType.SCHEDULE,
            claims = emptyList(),
        )

        val registry = ContextSourceRegistry()

        registry.register(source)

        assertEquals(
            1,
            registry.size,
        )

        assertTrue(
            registry.contains(
                ContextSourceId("schedule")
            )
        )

        assertNotNull(
            registry.find(
                ContextSourceId("schedule")
            )
        )
    }

    @Test
    fun registryCollectsAllSources() {
        val scheduleSource = StaticContextSource(
            id = ContextSourceId("schedule"),
            type = ContextSourceType.SCHEDULE,
            claims = emptyList(),
        )

        val userSource = StaticContextSource(
            id = ContextSourceId("user"),
            type = ContextSourceType.USER_INPUT,
            claims = emptyList(),
        )

        val registry = ContextSourceRegistry()

        registry.registerAll(
            listOf(
                scheduleSource,
                userSource,
            )
        )

        val results = registry.collectAll(
            ContextSourceInput(
                now = now,
            )
        )

        assertEquals(
            2,
            results.size,
        )

        assertEquals(
            listOf(
                ContextSourceId("schedule"),
                ContextSourceId("user"),
            ),
            results.map { it.sourceId },
        )
    }

    @Test
    fun duplicateSourceIdIsRejected() {
        val registry = ContextSourceRegistry()

        val first = StaticContextSource(
            id = ContextSourceId("same"),
            type = ContextSourceType.SYSTEM,
            claims = emptyList(),
        )

        val second = StaticContextSource(
            id = ContextSourceId("same"),
            type = ContextSourceType.SYSTEM,
            claims = emptyList(),
        )

        registry.register(first)

        val exception = runCatching {
            registry.register(second)
        }.exceptionOrNull()

        assertNotNull(exception)
    }
}
