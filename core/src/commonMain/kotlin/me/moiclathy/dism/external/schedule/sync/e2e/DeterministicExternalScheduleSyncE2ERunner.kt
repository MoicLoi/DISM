package me.moiclathy.dism.external.schedule.sync.e2e

import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionRequest
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionResult
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutor
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncRecoveryReconciliationCoordinator
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction

/**
 * 11.31 execution-level E2E:
 *
 * 11.28 Recovery + Reconciliation
 *          ↓
 * action mapping
 *          ↓
 * 11.29 Idempotency + 11.30 Action Executor
 *          ↓
 * Event Action Handler port
 *          ↓
 * observation
 *
 * This is deliberately not the final production Event integration because
 * the concrete 11.16 Event adapter API has not been assumed here.
 */
class DeterministicExternalScheduleSyncE2ERunner(
    private val recoveryReconciliationCoordinator:
        ExternalScheduleSyncRecoveryReconciliationCoordinator,
    private val actionExecutor: ExternalScheduleSyncActionExecutor,
) {

    fun run(
        input: ExternalScheduleSyncE2EInput,
    ): ExternalScheduleSyncE2EResult {
        val reconciliation = runCatching {
            recoveryReconciliationCoordinator.reconcile(
                scheduleId = input.scheduleId,
                runId = input.runId,
                seed = input.seed,
                projectedAt = input.projectedAt,
            )
        }.getOrElse { throwable ->
            return failure(
                message = "Recovery/Reconciliation boundary failed: " +
                    (throwable.message ?: throwable::class.simpleName),
            )
        }

        if (!reconciliation.successful) {
            return failure(
                message = reconciliation.errors.joinToString("; ").ifBlank {
                    "Recovery/Reconciliation returned unsuccessful"
                },
                warnings = reconciliation.warnings,
            )
        }

        val reconciliationResult = reconciliation.reconciliation
            ?: return failure(
                message = "Recovery/Reconciliation returned no reconciliation result",
                warnings = reconciliation.warnings,
            )

        val traces = mutableListOf<ExternalScheduleSyncE2EActionTrace>()

        for (record in reconciliationResult.records.sortedBy { it.externalKey }) {
            val trace = executeOne(
                input = input,
                record = record,
            )
            traces += trace
            input.observationSink.observe(trace)
        }

        val trace = ExternalScheduleSyncE2ETrace(
            scheduleId = input.scheduleId,
            runId = input.runId,
            reconciliation = reconciliation,
            actions = traces,
            phases = listOf(
                ExternalScheduleSyncE2EPhase.RECOVERY,
                ExternalScheduleSyncE2EPhase.RECONCILIATION,
                ExternalScheduleSyncE2EPhase.ACTION_MAPPING,
                ExternalScheduleSyncE2EPhase.IDEMPOTENCY,
                ExternalScheduleSyncE2EPhase.EVENT_ACTION,
                ExternalScheduleSyncE2EPhase.OBSERVATION,
                ExternalScheduleSyncE2EPhase.FINAL,
            ),
        )

        val contractErrors = ExternalScheduleSyncE2EContract.validate(trace)
        if (contractErrors.isNotEmpty()) {
            return ExternalScheduleSyncE2EResult(
                status = ExternalScheduleSyncE2EStatus.FAILED,
                trace = trace,
                warnings = reconciliation.warnings,
                errors = contractErrors,
            )
        }

        val warnings = reconciliation.warnings +
            traces.flatMap { it.warnings }

        val errors = reconciliation.errors +
            traces.flatMap { it.errors }

        val status = when {
            errors.isNotEmpty() && traces.none { it.successful } ->
                ExternalScheduleSyncE2EStatus.FAILED

            trace.actionCount == 0 ||
                (trace.failedCount == 0 &&
                    trace.executedCount == 0 &&
                    trace.skippedCount == 0 &&
                    trace.completeSyncCount == 0) ->
                ExternalScheduleSyncE2EStatus.NO_ACTIONS

            trace.failedCount == 0 ->
                ExternalScheduleSyncE2EStatus.COMPLETED

            traces.any { it.successful } ->
                ExternalScheduleSyncE2EStatus.PARTIAL

            else ->
                ExternalScheduleSyncE2EStatus.FAILED
        }

        return ExternalScheduleSyncE2EResult(
            status = status,
            trace = trace,
            warnings = warnings,
            errors = errors,
        )
    }

    private fun executeOne(
        input: ExternalScheduleSyncE2EInput,
        record: me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationRecord,
    ): ExternalScheduleSyncE2EActionTrace {
        return when (record.action) {
            ExternalScheduleSyncReconciliationAction.NO_OP ->
                ExternalScheduleSyncE2EActionTrace(
                    externalKey = record.externalKey,
                    reconciliationAction = record.action,
                    message = "No action required",
                )

            ExternalScheduleSyncReconciliationAction.COMPLETE_SYNC ->
                executeCompleteSync(input, record)

            ExternalScheduleSyncReconciliationAction.RETRY_CREATE ->
                executeEventAction(
                    input = input,
                    record = record,
                    action = ExternalScheduleSyncAction.CREATE_EVENT,
                )

            ExternalScheduleSyncReconciliationAction.RETRY_UPDATE ->
                executeEventAction(
                    input = input,
                    record = record,
                    action = ExternalScheduleSyncAction.UPDATE_EVENT,
                )

            ExternalScheduleSyncReconciliationAction.RETRY_REMOVAL ->
                executeEventAction(
                    input = input,
                    record = record,
                    action = ExternalScheduleSyncAction.HANDLE_REMOVED_EVENT,
                )

            ExternalScheduleSyncReconciliationAction.RECREATE_EVENT_MAPPING ->
                executeEventAction(
                    input = input,
                    record = record,
                    action = ExternalScheduleSyncAction.RECREATE_EVENT_MAPPING,
                )
        }
    }

    private fun executeCompleteSync(
        input: ExternalScheduleSyncE2EInput,
        record: me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationRecord,
    ): ExternalScheduleSyncE2EActionTrace {
        return runCatching {
            input.completeSyncHandler.complete(
                reconciliationRecord = record,
                now = input.projectedAt,
            )
        }.fold(
            onSuccess = { message ->
                ExternalScheduleSyncE2EActionTrace(
                    externalKey = record.externalKey,
                    reconciliationAction = record.action,
                    completeSyncHandled = true,
                    eventId = record.currentRecord?.eventId ?: record.syncRecord?.eventId,
                    message = message ?: "Sync completion repaired",
                )
            },
            onFailure = { throwable ->
                ExternalScheduleSyncE2EActionTrace(
                    externalKey = record.externalKey,
                    reconciliationAction = record.action,
                    message = throwable.message,
                    errors = listOf(
                        throwable.message ?: throwable::class.simpleName ?: "complete sync failed",
                    ),
                )
            },
        )
    }

    private fun executeEventAction(
        input: ExternalScheduleSyncE2EInput,
        record: me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationRecord,
        action: ExternalScheduleSyncAction,
    ): ExternalScheduleSyncE2EActionTrace {
        val current = record.currentRecord
        val previous = input.previousRecordResolver.resolve(
            externalKey = record.externalKey,
            currentRecord = current,
            reconciliationRecord = record,
            previousRecords = input.previousRecords,
        )

        val request = ExternalScheduleSyncActionExecutionRequest(
            scheduleId = input.scheduleId,
            runId = input.runId,
            externalKey = record.externalKey,
            action = action,
            previous = previous,
            current = current,
        )

        val result: ExternalScheduleSyncActionExecutionResult = runCatching {
            actionExecutor.execute(
                request = request,
                now = input.projectedAt,
            )
        }.getOrElse { throwable ->
            return ExternalScheduleSyncE2EActionTrace(
                externalKey = record.externalKey,
                reconciliationAction = record.action,
                syncAction = action,
                message = throwable.message,
                errors = listOf(
                    throwable.message ?: throwable::class.simpleName ?: "action execution failed",
                ),
            )
        }

        return ExternalScheduleSyncE2EActionTrace(
            externalKey = record.externalKey,
            reconciliationAction = record.action,
            syncAction = action,
            executionStatus = result.status,
            eventId = result.eventId,
            message = result.message,
            warnings = result.warnings,
            errors = result.errors,
        )
    }

    private fun failure(
        message: String,
        warnings: List<String> = emptyList(),
    ) = ExternalScheduleSyncE2EResult(
        status = ExternalScheduleSyncE2EStatus.FAILED,
        trace = null,
        warnings = warnings,
        errors = listOf(message),
    )
}
