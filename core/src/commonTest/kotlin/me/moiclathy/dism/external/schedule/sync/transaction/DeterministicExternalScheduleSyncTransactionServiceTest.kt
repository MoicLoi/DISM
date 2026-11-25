package me.moiclathy.dism.external.schedule.sync.transaction

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncTransactionServiceTest {

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
            "2026-09-10T02:00:00Z"
        )

    private fun run(
        id: String = "run-001"
    ) =
        ExternalScheduleSyncRunId(
            id
        )

    @Test
    fun `begin creates sync run`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        val result =
            service.begin(

                scheduleId =
                    scheduleId,

                runId =
                    run(),

                startedAt =
                    start,

                totalActions =
                    3,
            )

        assertEquals(
            ExternalScheduleSyncRunStatus
                .IN_PROGRESS,

            result.status
        )

        assertEquals(
            3,
            result.totalActions
        )

        assertEquals(
            0,
            result.completedActions
        )

        assertEquals(
            0,
            result.failedActions
        )
    }

    @Test
    fun `zero action run becomes no changes`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        val result =
            service.begin(

                scheduleId =
                    scheduleId,

                runId =
                    run(),

                startedAt =
                    start,

                totalActions =
                    0,
            )

        assertEquals(
            ExternalScheduleSyncRunStatus
                .NO_CHANGES,

            result.status
        )

        assertTrue(
            result.terminal
        )
    }

    @Test
    fun `completed actions advance run`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        service.begin(
            scheduleId,
            run(),
            start,
            2,
        )

        val first =
            service.actionCompleted(
                run(),
                end,
            )

        assertNotNull(
            first
        )

        assertEquals(
            1,
            first.completedActions
        )

        assertEquals(
            ExternalScheduleSyncRunStatus
                .IN_PROGRESS,

            first.status
        )

        val second =
            service.actionCompleted(
                run(),
                end,
            )

        assertNotNull(
            second
        )

        assertEquals(
            2,
            second.completedActions
        )

        assertEquals(
            ExternalScheduleSyncRunStatus
                .COMPLETED,

            second.status
        )
    }

    @Test
    fun `failed action makes run partial`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        service.begin(
            scheduleId,
            run(),
            start,
            2,
        )

        val result =
            service.actionFailed(
                run(),
                end,
                "event creation failed",
            )

        assertNotNull(
            result
        )

        assertEquals(
            1,
            result.failedActions
        )

        assertEquals(
            ExternalScheduleSyncRunStatus
                .PARTIAL,

            result.status
        )
    }

    @Test
    fun `recovery separates completed failed and pending`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        service.begin(
            scheduleId,
            run(),
            start,
            3,
        )

        val actions =
            listOf(

                ExternalScheduleSyncRunAction(

                    externalKey =
                        "a",

                    action =
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,

                    completed =
                        true,

                    eventId =
                        "event-a",
                ),

                ExternalScheduleSyncRunAction(

                    externalKey =
                        "b",

                    action =
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,

                    failed =
                        true,

                    message =
                        "update failed",
                ),

                ExternalScheduleSyncRunAction(

                    externalKey =
                        "c",

                    action =
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                ),
            )

        val result =
            service.recover(
                run(),
                actions,
            )

        assertTrue(
            result.successful
        )

        val plan =
            result.plan!!

        assertEquals(
            listOf("a"),
            plan.completedActions.map {
                it.externalKey
            }
        )

        assertEquals(
            listOf("b"),
            plan.failedActions.map {
                it.externalKey
            }
        )

        assertEquals(
            listOf("c"),
            plan.pendingActions.map {
                it.externalKey
            }
        )

        assertEquals(
            2,
            plan.recoverableCount
        )

        assertEquals(
            ExternalScheduleSyncRunStatus
                .RECOVERING,

            plan.originalRun.status
        )
    }

    @Test
    fun `begin is idempotent for same run`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        val first =
            service.begin(
                scheduleId,
                run(),
                start,
                3,
            )

        val second =
            service.begin(
                scheduleId,
                run(),
                end,
                99,
            )

        assertEquals(
            first,
            second
        )

        assertEquals(
            3,
            second.totalActions
        )
    }

    @Test
    fun `terminal run ignores later action completion`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        service.begin(
            scheduleId,
            run(),
            start,
            1,
        )

        val completed =
            service.actionCompleted(
                run(),
                end,
            )

        assertNotNull(
            completed
        )

        val later =
            service.actionCompleted(
                run(),
                end,
            )

        assertEquals(
            completed,
            later
        )
    }

    @Test
    fun `latest run is retrievable`() {

        val repository =
            InMemoryExternalScheduleSyncRunRepository()

        val service =
            DeterministicExternalScheduleSyncTransactionService(
                repository
            )

        service.begin(
            scheduleId,
            run("run-001"),
            start,
            1,
        )

        service.begin(
            scheduleId,
            run("run-002"),
            end,
            1,
        )

        val latest =
            repository.findLatest(
                scheduleId
            )

        assertNotNull(
            latest
        )

        assertEquals(
            "run-002",
            latest.runId.value
        )
    }
}
