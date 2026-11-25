package me.moiclathy.dism.external.schedule.sync.execution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.idempotency.DeterministicExternalScheduleSyncActionIdempotencyService
import me.moiclathy.dism.external.schedule.sync.idempotency.InMemoryExternalScheduleSyncActionIdempotencyRepository
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class DeterministicExternalScheduleSyncActionExecutorTest {

    private val scheduleId = ExternalScheduleId("schedule-11-30")
    private val runId = ExternalScheduleSyncRunId("run-11-30")
    private val now = Instant.parse("2026-09-19T02:00:00Z")

    private val current = ExternalSchedulePersistenceRecord(
        externalScheduleId = scheduleId,
        externalKey = "course-a",
        eventId = null,
        title = "Course A",
        startAt = now,
        endAt = now,
        lastSeenAt = now,
    )

    private val previous = current.copy(
        eventId = "event-old",
        title = "Course A old",
    )

    private fun executor(
        handler: ExternalScheduleSyncEventActionHandler,
    ) = DeterministicExternalScheduleSyncActionExecutor(
        idempotencyService =
            DeterministicExternalScheduleSyncActionIdempotencyService(
                InMemoryExternalScheduleSyncActionIdempotencyRepository(),
            ),
        handler = handler,
    )

    private fun request(
        action: ExternalScheduleSyncAction,
        previous: ExternalSchedulePersistenceRecord? = null,
        current: ExternalSchedulePersistenceRecord? = this.current,
    ) = ExternalScheduleSyncActionExecutionRequest(
        scheduleId = scheduleId,
        runId = runId,
        externalKey = "course-a",
        action = action,
        previous = previous,
        current = current,
    )

    @Test
    fun createExecutesExactlyOnce() {
        var calls = 0

        val executor = executor(
            object : ExternalScheduleSyncEventActionHandler {
                override fun create(current: ExternalSchedulePersistenceRecord): String? {
                    calls++
                    return "event-new"
                }

                override fun update(
                    previous: ExternalSchedulePersistenceRecord,
                    current: ExternalSchedulePersistenceRecord,
                ) = error("unexpected")

                override fun handleRemoved(previous: ExternalSchedulePersistenceRecord) =
                    error("unexpected")

                override fun recreateEventMapping(
                    previous: ExternalSchedulePersistenceRecord?,
                    current: ExternalSchedulePersistenceRecord?,
                ) = error("unexpected")
            },
        )

        val first = executor.execute(
            request(ExternalScheduleSyncAction.CREATE_EVENT),
            now,
        )
        val second = executor.execute(
            request(ExternalScheduleSyncAction.CREATE_EVENT),
            now,
        )

        assertEquals(ExternalScheduleSyncActionExecutionStatus.EXECUTED, first.status)
        assertEquals(ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_COMPLETED, second.status)
        assertEquals(1, calls)
        assertEquals("event-new", second.eventId)
    }

    @Test
    fun failedActionCanRetry() {
        var calls = 0

        val executor = executor(
            object : ExternalScheduleSyncEventActionHandler {
                override fun create(current: ExternalSchedulePersistenceRecord): String? {
                    calls++
                    if (calls == 1) error("temporary")
                    return "event-retried"
                }

                override fun update(
                    previous: ExternalSchedulePersistenceRecord,
                    current: ExternalSchedulePersistenceRecord,
                ) = error("unexpected")

                override fun handleRemoved(previous: ExternalSchedulePersistenceRecord) =
                    error("unexpected")

                override fun recreateEventMapping(
                    previous: ExternalSchedulePersistenceRecord?,
                    current: ExternalSchedulePersistenceRecord?,
                ) = error("unexpected")
            },
        )

        val first = executor.execute(
            request(ExternalScheduleSyncAction.CREATE_EVENT),
            now,
        )
        val second = executor.execute(
            request(ExternalScheduleSyncAction.CREATE_EVENT),
            now,
        )

        assertEquals(ExternalScheduleSyncActionExecutionStatus.FAILED, first.status)
        assertEquals(ExternalScheduleSyncActionExecutionStatus.EXECUTED, second.status)
        assertEquals(2, calls)
        assertEquals("event-retried", second.eventId)
    }

    @Test
    fun updateRequiresBothRecords() {
        val executor = executor(
            object : ExternalScheduleSyncEventActionHandler {
                override fun create(current: ExternalSchedulePersistenceRecord) = "unused"

                override fun update(
                    previous: ExternalSchedulePersistenceRecord,
                    current: ExternalSchedulePersistenceRecord,
                ) = "event-updated"

                override fun handleRemoved(previous: ExternalSchedulePersistenceRecord) = "unused"

                override fun recreateEventMapping(
                    previous: ExternalSchedulePersistenceRecord?,
                    current: ExternalSchedulePersistenceRecord?,
                ) = "unused"
            },
        )

        val result = executor.execute(
            request(
                action = ExternalScheduleSyncAction.UPDATE_EVENT,
                previous = previous,
                current = current,
            ),
            now,
        )

        assertEquals(ExternalScheduleSyncActionExecutionStatus.EXECUTED, result.status)
        assertEquals("event-updated", result.eventId)
    }

    @Test
    fun noOpIsRejectedAndDoesNotExecuteHandler() {
        var called = false

        val executor = executor(
            object : ExternalScheduleSyncEventActionHandler {
                override fun create(current: ExternalSchedulePersistenceRecord): String? {
                    called = true
                    return "unexpected"
                }

                override fun update(
                    previous: ExternalSchedulePersistenceRecord,
                    current: ExternalSchedulePersistenceRecord,
                ) = "unexpected"

                override fun handleRemoved(previous: ExternalSchedulePersistenceRecord) = "unexpected"

                override fun recreateEventMapping(
                    previous: ExternalSchedulePersistenceRecord?,
                    current: ExternalSchedulePersistenceRecord?,
                ) = "unexpected"
            },
        )

        val result = executor.execute(
            request(ExternalScheduleSyncAction.NO_OP),
            now,
        )

        assertEquals(ExternalScheduleSyncActionExecutionStatus.REJECTED, result.status)
        assertFalse(called)
    }

    @Test
    fun removedActionUsesPreviousRecord() {
        var received: ExternalSchedulePersistenceRecord? = null

        val executor = executor(
            object : ExternalScheduleSyncEventActionHandler {
                override fun create(current: ExternalSchedulePersistenceRecord) = "unexpected"

                override fun update(
                    previous: ExternalSchedulePersistenceRecord,
                    current: ExternalSchedulePersistenceRecord,
                ) = "unexpected"

                override fun handleRemoved(previous: ExternalSchedulePersistenceRecord): String? {
                    received = previous
                    return "event-old"
                }

                override fun recreateEventMapping(
                    previous: ExternalSchedulePersistenceRecord?,
                    current: ExternalSchedulePersistenceRecord?,
                ) = "unexpected"
            },
        )

        val result = executor.execute(
            request(
                action = ExternalScheduleSyncAction.HANDLE_REMOVED_EVENT,
                previous = previous,
                current = null,
            ),
            now,
        )

        assertEquals(ExternalScheduleSyncActionExecutionStatus.EXECUTED, result.status)
        assertEquals("event-old", received?.eventId)
        assertTrue(result.successful)
    }
}
