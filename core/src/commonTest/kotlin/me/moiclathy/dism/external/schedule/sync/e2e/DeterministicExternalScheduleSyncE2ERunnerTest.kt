package me.moiclathy.dism.external.schedule.sync.e2e

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionResult
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionStatus
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutor
import me.moiclathy.dism.external.schedule.sync.idempotency.ExternalScheduleSyncActionExecutionRecord
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncRecoveryReconciliationCoordinator
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncRecoveryReconciliationResult
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncRecoveryReconciliationStatus
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationRecord
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationReason
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationResult
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationStatus
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryResult
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryStatus
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunPhase
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunStatus

class DeterministicExternalScheduleSyncE2ERunnerTest {
    private val scheduleId = ExternalScheduleId("schedule-1")
    private val runId = ExternalScheduleSyncRunId("run-1")
    private val now = Instant.fromEpochMilliseconds(1_000L)

    private val seed = ExternalScheduleSyncRunRecord(
        scheduleId = scheduleId,
        runId = runId,
        status = ExternalScheduleSyncRunStatus.IN_PROGRESS,
        phase = ExternalScheduleSyncRunPhase.EXECUTING,
        startedAt = now,
        updatedAt = now,
        totalActions = 1,
    )

    @Test
    fun noOpDoesNotEnterActionExecutor() {
        var executorCalls = 0
        val runner = runner(
            reconciliationAction = ExternalScheduleSyncReconciliationAction.NO_OP,
            executor = object : ExternalScheduleSyncActionExecutor {
                override fun execute(
                    request: me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionRequest,
                    now: kotlin.time.Instant,
                ): ExternalScheduleSyncActionExecutionResult {
                    executorCalls++
                    error("NO_OP must not reach executor")
                }
            },
        )

        val result = runner.run(input())

        assertEquals(ExternalScheduleSyncE2EStatus.NO_ACTIONS, result.status)
        assertEquals(0, executorCalls)
        assertEquals(1, result.trace?.noOpCount)
    }

    @Test
    fun recreateEventMappingReachesActionExecutorBoundary() {
        var executorCalls = 0
        val executor = object : ExternalScheduleSyncActionExecutor {
            override fun execute(
                request: me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionRequest,
                now: kotlin.time.Instant,
            ): ExternalScheduleSyncActionExecutionResult {
                executorCalls++
                assertEquals(ExternalScheduleSyncAction.RECREATE_EVENT_MAPPING, request.action)
                return ExternalScheduleSyncActionExecutionResult(
                    status = ExternalScheduleSyncActionExecutionStatus.EXECUTED,
                    externalKey = request.externalKey,
                    action = request.action.name,
                    eventId = "event-1",
                )
            }
        }

        val runner = runner(
            reconciliationAction = ExternalScheduleSyncReconciliationAction.RECREATE_EVENT_MAPPING,
            executor = executor,
        )

        val result = runner.run(input())

        assertEquals(ExternalScheduleSyncE2EStatus.COMPLETED, result.status)
        assertEquals(1, executorCalls)
    }

    @Test
    fun failedCreateIsExposedAsPartialOrFailed() {
        val runner = runner(
            reconciliationAction = ExternalScheduleSyncReconciliationAction.RETRY_CREATE,
            executor = object : ExternalScheduleSyncActionExecutor {
                override fun execute(
                    request: me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionRequest,
                    now: kotlin.time.Instant,
                ): ExternalScheduleSyncActionExecutionResult =
                    ExternalScheduleSyncActionExecutionResult(
                        status = ExternalScheduleSyncActionExecutionStatus.FAILED,
                        externalKey = request.externalKey,
                        action = request.action.name,
                        errors = listOf("create failed"),
                    )
            },
        )

        val result = runner.run(input(current = record("k1")))

        assertTrue(
            result.status == ExternalScheduleSyncE2EStatus.FAILED ||
                result.status == ExternalScheduleSyncE2EStatus.PARTIAL,
        )
        assertEquals(1, result.failedCount)
    }

    @Test
    fun updateReceivesPreviousRecord() {
        val previous = record("k1", title = "old")
        val current = record("k1", title = "new")
        var receivedPrevious: ExternalSchedulePersistenceRecord? = null

        val runner = runner(
            reconciliationAction = ExternalScheduleSyncReconciliationAction.RETRY_UPDATE,
            executor = object : ExternalScheduleSyncActionExecutor {
                override fun execute(
                    request: me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionRequest,
                    now: kotlin.time.Instant,
                ): ExternalScheduleSyncActionExecutionResult {
                    receivedPrevious = request.previous
                    return ExternalScheduleSyncActionExecutionResult(
                        status = ExternalScheduleSyncActionExecutionStatus.EXECUTED,
                        externalKey = request.externalKey,
                        action = request.action.name,
                        eventId = "event-1",
                    )
                }
            },
        )

        val result = runner.run(input(current = current, previous = listOf(previous)))

        assertEquals(ExternalScheduleSyncE2EStatus.COMPLETED, result.status)
        assertEquals(previous, receivedPrevious)
    }

    private fun runner(
        reconciliationAction: ExternalScheduleSyncReconciliationAction,
        executor: ExternalScheduleSyncActionExecutor,
    ): DeterministicExternalScheduleSyncE2ERunner {
        val coordinator = object : ExternalScheduleSyncRecoveryReconciliationCoordinator {
            override fun reconcile(
                scheduleId: ExternalScheduleId,
                runId: ExternalScheduleSyncRunId,
                seed: ExternalScheduleSyncRunRecord,
                projectedAt: kotlin.time.Instant,
            ): ExternalScheduleSyncRecoveryReconciliationResult =
                ExternalScheduleSyncRecoveryReconciliationResult(
                status = ExternalScheduleSyncRecoveryReconciliationStatus.RECONCILED,
                recovery = ExternalScheduleSyncRunRecoveryResult(
                    scheduleId = scheduleId,
                    runId = runId,
                    status = ExternalScheduleSyncRunRecoveryStatus.RESTORED,
                ),
                reconciliation = ExternalScheduleSyncReconciliationResult.success(
                    records = listOf(
                        ExternalScheduleSyncReconciliationRecord(
                            scheduleId = scheduleId,
                            externalKey = "k1",
                            status = when (reconciliationAction) {
                                ExternalScheduleSyncReconciliationAction.NO_OP ->
                                    ExternalScheduleSyncReconciliationStatus.CONSISTENT
                                ExternalScheduleSyncReconciliationAction.COMPLETE_SYNC ->
                                    ExternalScheduleSyncReconciliationStatus.EVENT_EXISTS_BUT_SYNC_INCOMPLETE
                                ExternalScheduleSyncReconciliationAction.RETRY_CREATE,
                                ExternalScheduleSyncReconciliationAction.RETRY_UPDATE,
                                ExternalScheduleSyncReconciliationAction.RETRY_REMOVAL,
                                ExternalScheduleSyncReconciliationAction.RECREATE_EVENT_MAPPING ->
                                    ExternalScheduleSyncReconciliationStatus.SYNC_INCOMPLETE_EVENT_MISSING
                            },
                            action = reconciliationAction,
                            reason = ExternalScheduleSyncReconciliationReason.SYNC_FAILED_EVENT_MISSING,
                            currentRecord = null,
                            eventExists = false,
                        ),
                    ),
                ),
            )
        }
        return DeterministicExternalScheduleSyncE2ERunner(
            recoveryReconciliationCoordinator = coordinator,
            actionExecutor = executor,
        )
    }

    private fun input(
        current: ExternalSchedulePersistenceRecord? = null,
        previous: List<ExternalSchedulePersistenceRecord> = emptyList(),
    ) = ExternalScheduleSyncE2EInput(
        scheduleId = scheduleId,
        runId = runId,
        seed = seed,
        projectedAt = now,
        previousRecords = previous,
        previousRecordResolver = ExternalScheduleSyncPreviousRecordResolver { externalKey, _, _, records ->
            records.firstOrNull { it.externalKey == externalKey }
        },
        completeSyncHandler = ExternalScheduleSyncCompleteHandler { _, _ -> "completed" },
    )

    private fun record(
        key: String,
        title: String = "course",
    ) = ExternalSchedulePersistenceRecord(
        externalScheduleId = scheduleId,
        externalKey = key,
        eventId = null,
        title = title,
        startAt = null,
        endAt = null,
        lastSeenAt = now,
        active = true,
    )
}
