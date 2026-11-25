package me.moiclathy.dism.external.schedule.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExternalSchedulePersistenceServiceTest {

    private val repository =
        InMemoryExternalSchedulePersistenceRepository()

    private val service =
        ExternalSchedulePersistenceService(
            repository
        )

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val importedAt =
        Instant.parse(
            "2026-09-01T00:00:00Z"
        )

    private fun record(
        key: String,
        title: String = "实验动物学",
        active: Boolean = true,
    ): ExternalSchedulePersistenceRecord {

        return ExternalSchedulePersistenceRecord(
            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                "event-$key",

            title =
                title,

            startAt =
                Instant.parse(
                    "2026-09-01T01:00:00Z"
                ),

            endAt =
                Instant.parse(
                    "2026-09-01T02:00:00Z"
                ),

            locationEntityId =
                "location-001",

            instructorEntityId =
                "person-001",

            courseEntityId =
                "course-001",

            lastSeenAt =
                importedAt,

            active =
                active,

            fingerprint =
                "fingerprint-$key",
        )
    }

    @Test
    fun `record can be persisted`() {

        val result =
            repository.upsert(
                record("entry-001")
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.affectedRecords
        )

        assertNotNull(
            repository.find(
                scheduleId,
                "entry-001"
            )
        )
    }

    @Test
    fun `upsert replaces existing record`() {

        repository.upsert(
            record(
                key = "entry-001",
                title = "旧课程",
            )
        )

        repository.upsert(
            record(
                key = "entry-001",
                title = "新课程",
            )
        )

        val stored =
            repository.find(
                scheduleId,
                "entry-001"
            )

        assertNotNull(stored)

        assertEquals(
            "新课程",
            stored.title
        )
    }

    @Test
    fun `snapshot persists all records`() {

        val snapshot =
            ExternalSchedulePersistenceSnapshot(
                scheduleId =
                    scheduleId,

                importedAt =
                    importedAt,

                records =
                    listOf(
                        record("entry-001"),
                        record("entry-002"),
                        record("entry-003"),
                    ),
            )

        val result =
            service.persistSnapshot(
                snapshot
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            3,
            service.findBySchedule(
                scheduleId
            ).size
        )
    }

    @Test
    fun `missing records become inactive`() {

        repository.upsert(
            record("entry-001")
        )

        repository.upsert(
            record("entry-002")
        )

        val snapshot =
            ExternalSchedulePersistenceSnapshot(
                scheduleId =
                    scheduleId,

                importedAt =
                    Instant.parse(
                        "2026-09-02T00:00:00Z"
                    ),

                records =
                    listOf(
                        record("entry-001")
                    ),
            )

        val result =
            service.persistSnapshot(
                snapshot
            )

        assertTrue(
            result.successful
        )

        val oldRecord =
            repository.find(
                scheduleId,
                "entry-002"
            )

        assertNotNull(
            oldRecord
        )

        assertFalse(
            oldRecord.active
        )
    }

    @Test
    fun `existing event id survives reimport`() {

        val original =
            record("entry-001")

        repository.upsert(
            original
        )

        val reimported =
            original.copy(
                title =
                    "实验动物学（更新）",
            )

        repository.upsert(
            reimported
        )

        val stored =
            repository.find(
                scheduleId,
                "entry-001"
            )

        assertNotNull(
            stored
        )

        assertEquals(
            original.eventId,
            stored.eventId
        )
    }

    @Test
    fun `different schedules do not collide`() {

        val scheduleA =
            ExternalScheduleId(
                "schedule-a"
            )

        val scheduleB =
            ExternalScheduleId(
                "schedule-b"
            )

        repository.upsert(
            record(
                key = "same-entry"
            ).copy(
                externalScheduleId =
                    scheduleA
            )
        )

        repository.upsert(
            record(
                key = "same-entry"
            ).copy(
                externalScheduleId =
                    scheduleB
            )
        )

        assertNotNull(
            repository.find(
                scheduleA,
                "same-entry"
            )
        )

        assertNotNull(
            repository.find(
                scheduleB,
                "same-entry"
            )
        )
    }

    @Test
    fun `inactive record can be reactivated`() {

        repository.upsert(
            record(
                key = "entry-001",
                active = false,
            )
        )

        repository.upsert(
            record(
                key = "entry-001",
                active = true,
            )
        )

        val stored =
            repository.find(
                scheduleId,
                "entry-001"
            )

        assertNotNull(
            stored
        )

        assertTrue(
            stored.active
        )
    }
}
