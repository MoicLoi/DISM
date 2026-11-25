package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TemporalTimelineTest {

    private fun state(
        itemId: Uuid,
        objectId: Uuid,
        from: String,
        until: String? = null,
        status: RelationStatus = RelationStatus.ACTIVE,
    ): RelationState {
        return RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = objectId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse(from),
            validUntil = until?.let(Instant::parse),
            status = status,
        )
    }

    @Test
    fun timeline_shouldSortAndQueryHistoricalState() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val lab = Uuid.random()

        val timeline = TemporalTimeline.of(
            listOf(
                state(
                    item,
                    lab,
                    "2026-09-08T10:00:00Z",
                ),
                state(
                    item,
                    dorm,
                    "2026-09-08T08:00:00Z",
                    "2026-09-08T10:00:00Z",
                    RelationStatus.SUPERSEDED,
                ),
            )
        )

        assertEquals(
            dorm,
            timeline.at(
                Instant.parse("2026-09-08T09:00:00Z")
            ).single().key.objectId,
        )

        assertEquals(
            lab,
            timeline.at(
                Instant.parse("2026-09-08T10:00:00Z")
            ).single().key.objectId,
        )

        assertEquals(1, timeline.current().size)
        assertEquals(lab, timeline.current().single().key.objectId)
    }

    @Test
    fun timeline_shouldTreatIntervalsAsHalfOpen() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val lab = Uuid.random()

        val timeline = TemporalTimeline.of(
            listOf(
                state(
                    item,
                    dorm,
                    "2026-09-08T08:00:00Z",
                    "2026-09-08T10:00:00Z",
                    RelationStatus.SUPERSEDED,
                ),
                state(
                    item,
                    lab,
                    "2026-09-08T10:00:00Z",
                ),
            )
        )

        assertTrue(
            timeline.at(
                Instant.parse("2026-09-08T09:59:59Z")
            ).single().key.objectId == dorm
        )

        assertTrue(
            timeline.at(
                Instant.parse("2026-09-08T10:00:00Z")
            ).single().key.objectId == lab
        )
    }

    @Test
    fun timeline_shouldReportChangesAtBoundary() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val lab = Uuid.random()
        val changeAt = Instant.parse("2026-09-08T10:00:00Z")

        val timeline = TemporalTimeline.of(
            listOf(
                state(
                    item,
                    dorm,
                    "2026-09-08T08:00:00Z",
                    "2026-09-08T10:00:00Z",
                    RelationStatus.SUPERSEDED,
                ),
                state(
                    item,
                    lab,
                    "2026-09-08T10:00:00Z",
                ),
            )
        )

        val changes = timeline.changesAt(changeAt)

        assertEquals(2, changes.size)
        assertTrue(changes.any { it is me.moiclathy.dism.state.query.TemporalStateChange.Ended })
        assertTrue(changes.any { it is me.moiclathy.dism.state.query.TemporalStateChange.Started })
    }

    @Test
    fun timeline_shouldFindIntersectingWindow() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val lab = Uuid.random()

        val timeline = TemporalTimeline.of(
            listOf(
                state(
                    item,
                    dorm,
                    "2026-09-08T08:00:00Z",
                    "2026-09-08T10:00:00Z",
                    RelationStatus.SUPERSEDED,
                ),
                state(
                    item,
                    lab,
                    "2026-09-08T10:00:00Z",
                ),
            )
        )

        val result = timeline.intersecting(
            start = Instant.parse("2026-09-08T09:30:00Z"),
            end = Instant.parse("2026-09-08T11:00:00Z"),
        )

        assertEquals(2, result.size)
    }
}
