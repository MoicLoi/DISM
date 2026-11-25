package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncIdempotencyServiceTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val start =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private val end =
        Instant.parse(
            "2026-09-10T01:01:00Z"
        )

    private fun key(
        externalKey: String = "entry-001",
        runId: String = "run-001",
        action: String = "CREATE_EVENT",
    ) =
        ExternalScheduleSyncIdempotencyKey(

            scheduleId =
                scheduleId,

            externalKey =
                externalKey,

            syncRunId =
                runId,

            action =
                action,
        )

    @Test
    fun `new action can begin`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        val result =
            service.begin(
                key =
                    key(),

                startedAt =
                    start,
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            true,
            result.value
        )

        val record =
            service.find(
                key()
            )

        assertNotNull(
            record
        )

        assertEquals(
            ExternalScheduleSyncIdempotencyStatus
                .IN_PROGRESS,

            record.status
        )
    }

    @Test
    fun `completed action cannot execute twice`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        val first =
            service.begin(
                key(),
                start
            )

        assertTrue(
            first.value == true
        )

        service.complete(
            key(),
            end,
            eventId = "event-001",
        )

        val second =
            service.begin(
                key(),
                end,
            )

        assertTrue(
            second.successful
        )

        assertFalse(
            second.value == true
        )

        assertTrue(
            second.reusedExistingResult
        )
    }

    @Test
    fun `failed action can be retried`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        service.begin(
            key(),
            start
        )

        service.fail(
            key(),
            end,
            "temporary failure",
        )

        val retry =
            service.begin(
                key(),
                end,
            )

        assertTrue(
            retry.successful
        )

        assertEquals(
            true,
            retry.value
        )

        val record =
            service.find(
                key()
            )

        assertNotNull(
            record
        )

        assertEquals(
            ExternalScheduleSyncIdempotencyStatus
                .IN_PROGRESS,

            record.status
        )
    }

    @Test
    fun `in progress action is not duplicated`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        service.begin(
            key(),
            start
        )

        val second =
            service.begin(
                key(),
                end
            )

        assertTrue(
            second.successful
        )

        assertFalse(
            second.value == true
        )

        assertTrue(
            second.reusedExistingResult
        )
    }

    @Test
    fun `complete stores event mapping`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        service.begin(
            key(),
            start
        )

        val result =
            service.complete(
                key(),
                end,
                eventId = "event-123",
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            "event-123",
            result.value!!
                .eventId
        )

        assertEquals(
            ExternalScheduleSyncIdempotencyStatus
                .COMPLETED,

            result.value.status
        )
    }

    @Test
    fun `cannot complete unknown key`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        val result =
            service.complete(
                key(),
                end,
            )

        assertFalse(
            result.successful
        )
    }

    @Test
    fun `cannot overwrite completed action with failure`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        service.begin(
            key(),
            start
        )

        service.complete(
            key(),
            end,
            eventId = "event-001",
        )

        val result =
            service.fail(
                key(),
                end,
                "late failure",
            )

        assertFalse(
            result.successful
        )

        assertEquals(
            ExternalScheduleSyncIdempotencyStatus
                .COMPLETED,

            service.find(
                key()
            )!!.status
        )
    }

    @Test
    fun `different runs are independent`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        val firstKey =
            key(
                runId = "run-001"
            )

        val secondKey =
            key(
                runId = "run-002"
            )

        service.begin(
            firstKey,
            start
        )

        service.complete(
            firstKey,
            end,
            eventId = "event-001",
        )

        val second =
            service.begin(
                secondKey,
                end
            )

        assertTrue(
            second.value == true
        )
    }

    @Test
    fun `different actions are independent`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        val create =
            key(
                action = "CREATE_EVENT"
            )

        val update =
            key(
                action = "UPDATE_EVENT"
            )

        service.begin(
            create,
            start
        )

        service.complete(
            create,
            end,
            eventId = "event-001",
        )

        val result =
            service.begin(
                update,
                end
            )

        assertTrue(
            result.value == true
        )
    }

    @Test
    fun `find by run returns sorted records`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        val keyA =
            key(
                externalKey = "a"
            )

        val keyB =
            key(
                externalKey = "b"
            )

        service.begin(
            keyB,
            start
        )

        service.begin(
            keyA,
            start
        )

        val records =
            repository.findByRun(
                "run-001"
            )

        assertEquals(
            listOf(
                "a",
                "b",
            ),
            records.map {
                it.key.externalKey
            }
        )
    }

    @Test
    fun `delete removes idempotency record`() {

        val repository =
            InMemoryExternalScheduleSyncIdempotencyRepository()

        val service =
            DeterministicExternalScheduleSyncIdempotencyService(
                repository
            )

        service.begin(
            key(),
            start
        )

        assertNotNull(
            service.find(
                key()
            )
        )

        repository.delete(
            key()
        )

        assertNull(
            service.find(
                key()
            )
        )
    }
}
