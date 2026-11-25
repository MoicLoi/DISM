package me.moiclathy.dism.semantic.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ResolutionMemoryTest {

    @Test
    fun recentReferences_shouldBeReturnedNewestFirst() {
        val memory = ResolutionMemory()

        val firstId = Uuid.random()
        val secondId = Uuid.random()

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")
        val secondTime = Instant.parse("2026-09-08T08:01:00Z")

        memory.record(
            entityId = firstId,
            referencedAt = firstTime,
        )

        memory.record(
            entityId = secondId,
            referencedAt = secondTime,
        )

        val result = memory.recent(
            now = Instant.parse("2026-09-08T08:02:00Z")
        )

        assertEquals(secondId, result[0].entityId)
        assertEquals(firstId, result[1].entityId)
    }

    @Test
    fun futureReference_shouldBeExcluded() {
        val memory = ResolutionMemory()

        val entityId = Uuid.random()

        memory.record(
            entityId = entityId,
            referencedAt = Instant.parse("2026-09-08T08:30:00Z"),
        )

        val result = memory.recent(
            now = Instant.parse("2026-09-08T08:00:00Z")
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun recent_shouldRespectLimit() {
        val memory = ResolutionMemory()

        val now = Instant.parse("2026-09-08T08:00:00Z")

        repeat(5) { index ->
            memory.record(
                entityId = Uuid.random(),
                referencedAt = now - (index + 1).toLong().seconds,
            )
        }

        val result = memory.recent(
            now = now,
            limit = 3,
        )

        assertEquals(3, result.size)
    }

    @Test
    fun recent_shouldKeepLatestReferencePerEntity() {
        val memory = ResolutionMemory()

        val entityId = Uuid.random()

        val first = Instant.parse("2026-09-08T08:00:00Z")
        val second = Instant.parse("2026-09-08T08:01:00Z")

        memory.record(
            entityId = entityId,
            referencedAt = first,
            confidence = 0.8,
        )

        memory.record(
            entityId = entityId,
            referencedAt = second,
            confidence = 1.0,
        )

        val result = memory.recent(now = second)

        assertEquals(1, result.size)
        assertEquals(second, result.single().referencedAt)
        assertEquals(1.0, result.single().confidence)
    }
}
