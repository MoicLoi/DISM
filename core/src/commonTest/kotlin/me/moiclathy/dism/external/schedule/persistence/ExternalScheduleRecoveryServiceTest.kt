package me.moiclathy.dism.external.schedule.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExternalScheduleRecoveryServiceTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val importedAt =
        Instant.parse(
            "2026-09-10T00:00:00Z"
        )

    private val repository =
        InMemoryExternalScheduleRecoveryRepository()

    private val service =
        ExternalScheduleRecoveryService(
            repository
        )

    private fun record(
        key: String,
        eventId: String? = "event-$key",
        active: Boolean = true,
    ): ExternalSchedulePersistenceRecord {

        return ExternalSchedulePersistenceRecord(

            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                eventId,

            title =
                "实验动物学",

            startAt =
                Instant.parse(
                    "2026-09-10T01:00:00Z"
                ),

            endAt =
                Instant.parse(
                    "2026-09-10T02:00:00Z"
                ),

            lastSeenAt =
                importedAt,

            active =
                active,

            fingerprint =
                "fingerprint-$key",
        )
    }

    @Test
    fun `empty schedule can be recovered`() {

        val result =
            service.recover(
                scheduleId
            )

        assertTrue(
            result.successful
        )

        assertNotNull(
            result.snapshot
        )

        assertEquals(
            0,
            result.snapshot!!.size
        )

        assertEquals(
            1,
            result.warnings.size
        )
    }

    @Test
    fun `active record with event mapping is restored`() {

        repository.add(
            record(
                key = "entry-001"
            )
        )

        val result =
            service.recover(
                scheduleId
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.restoredCount
        )

        assertEquals(
            0,
            result.invalidCount
        )

        val restored =
            result.snapshot!!
                .restoredRecords
                .single()

        assertEquals(
            "event-entry-001",
            restored.record.eventId
        )

        assertTrue(
            restored.recoverable
        )
    }

    @Test
    fun `active record without event mapping is recoverable`() {

        repository.add(
            record(
                key = "entry-001",
                eventId = null,
            )
        )

        val result =
            service.recover(
                scheduleId
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.missingEventMappingCount
        )

        val recovered =
            result.snapshot!!
                .missingEventMappings
                .single()

        assertTrue(
            recovered.recoverable
        )

        assertEquals(
            ExternalScheduleRecoveryStatus
                .MISSING_EVENT_MAPPING,
            recovered.status
        )
    }

    @Test
    fun `inactive record is not restored`() {

        repository.add(
            record(
                key = "entry-001",
                active = false,
            )
        )

        val result =
            service.recover(
                scheduleId
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.inactiveCount
        )

        assertEquals(
            0,
            result.restoredCount
        )

        val recovered =
            result.snapshot!!
                .inactiveRecords
                .single()

        assertFalse(
            recovered.recoverable
        )
    }

    @Test
    fun `multiple records are recovered independently`() {

        repository.addAll(
            listOf(
                record(
                    key = "entry-001"
                ),

                record(
                    key = "entry-002",
                    eventId = null,
                ),

                record(
                    key = "entry-003",
                    active = false,
                ),
            )
        )

        val result =
            service.recover(
                scheduleId
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.restoredCount
        )

        assertEquals(
            1,
            result.missingEventMappingCount
        )

        assertEquals(
            1,
            result.inactiveCount
        )
    }

    @Test
    fun `recovery does not mutate repository`() {

        val original =
            record(
                key = "entry-001"
            )

        repository.add(
            original
        )

        service.recover(
            scheduleId
        )

        val stored =
            repository.findBySchedule(
                scheduleId
            ).single()

        assertEquals(
            original,
            stored
        )
    }

    @Test
    fun `recovery is deterministic`() {

        repository.addAll(
            listOf(
                record("entry-001"),
                record(
                    "entry-002",
                    eventId = null
                ),
                record(
                    "entry-003",
                    active = false
                ),
            )
        )

        val first =
            service.recover(
                scheduleId
            )

        val second =
            service.recover(
                scheduleId
            )

        assertEquals(
            first,
            second
        )
    }
}
