package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class DeterministicExternalScheduleSyncActionIdempotencyServiceTest {

    private val scheduleId = ExternalScheduleId("schedule-11-29")
    private val runId = ExternalScheduleSyncRunId("run-11-29")
    private val now = Instant.parse("2026-09-19T02:00:00Z")

    private fun identity(
        key: String = "course-a",
        action: ExternalScheduleSyncAction = ExternalScheduleSyncAction.CREATE_EVENT,
    ) = ExternalScheduleSyncActionIdentity(
        scheduleId = scheduleId,
        runId = runId,
        externalKey = key,
        action = action,
    )

    @Test
    fun sameIdentityStartsOnlyOnce() {
        val service = DeterministicExternalScheduleSyncActionIdempotencyService(
            InMemoryExternalScheduleSyncActionIdempotencyRepository(),
        )
        val id = identity()

        val first = service.begin(id, now)
        val second = service.begin(id, now)

        assertEquals(
            ExternalScheduleSyncActionIdempotencyDecision.STARTED,
            first.decision,
        )
        assertEquals(
            ExternalScheduleSyncActionIdempotencyDecision.ALREADY_IN_PROGRESS,
            second.decision,
        )
        assertTrue(first.shouldExecute)
        assertFalse(second.shouldExecute)
    }

    @Test
    fun completedIdentityCannotExecuteAgain() {
        val service = DeterministicExternalScheduleSyncActionIdempotencyService(
            InMemoryExternalScheduleSyncActionIdempotencyRepository(),
        )
        val id = identity()

        service.begin(id, now)
        val completed = service.complete(
            identity = id,
            now = now,
            eventId = "event-a",
        )
        val retry = service.begin(id, now)

        assertEquals(
            ExternalScheduleSyncActionIdempotencyDecision.STARTED,
            completed.decision,
        )
        assertEquals(
            ExternalScheduleSyncActionIdempotencyDecision.ALREADY_COMPLETED,
            retry.decision,
        )
        assertFalse(retry.shouldExecute)
        assertEquals("event-a", retry.record?.eventId)
    }

    @Test
    fun failedIdentityCanBeRetriedWithSameIdentity() {
        val service = DeterministicExternalScheduleSyncActionIdempotencyService(
            InMemoryExternalScheduleSyncActionIdempotencyRepository(),
        )
        val id = identity()

        service.begin(id, now)
        service.fail(id, now, "simulated failure")
        val retry = service.begin(id, now)

        assertEquals(
            ExternalScheduleSyncActionIdempotencyDecision.RETRY_ALLOWED,
            retry.decision,
        )
        assertTrue(retry.shouldExecute)
        assertEquals(
            ExternalScheduleSyncActionExecutionStatus.IN_PROGRESS,
            retry.record?.status,
        )
    }

    @Test
    fun differentActionsHaveDifferentIdentities() {
        val create = identity(action = ExternalScheduleSyncAction.CREATE_EVENT)
        val update = identity(action = ExternalScheduleSyncAction.UPDATE_EVENT)

        assertFalse(create.value == update.value)
    }

    @Test
    fun unknownActionCannotBeCompleted() {
        val service = DeterministicExternalScheduleSyncActionIdempotencyService(
            InMemoryExternalScheduleSyncActionIdempotencyRepository(),
        )

        val result = service.complete(identity(), now)

        assertEquals(
            ExternalScheduleSyncActionIdempotencyDecision.REJECTED,
            result.decision,
        )
        assertTrue(result.errors.isNotEmpty())
    }
}
