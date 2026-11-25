package me.moiclathy.dism.external.schedule.sync.execution

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.idempotency.DeterministicExternalScheduleSyncActionIdempotencyService
import me.moiclathy.dism.external.schedule.sync.idempotency.ExternalScheduleSyncActionIdempotencyDecision

/**
 * 11.30 execution boundary.
 *
 * Flow:
 *
 * 11.28 reconciliation decision
 *          ↓
 * 11.29 idempotency gate
 *          ↓
 * 11.30 handler execution
 *
 * The handler is the only component allowed to cross into the Event layer.
 */
class DeterministicExternalScheduleSyncActionExecutor(
    private val idempotencyService: DeterministicExternalScheduleSyncActionIdempotencyService,
    private val handler: ExternalScheduleSyncEventActionHandler,
) : ExternalScheduleSyncActionExecutor {

    override fun execute(
        request: ExternalScheduleSyncActionExecutionRequest,
        now: Instant,
    ): ExternalScheduleSyncActionExecutionResult {
        val gate = idempotencyService.begin(request.identity, now)

        when (gate.decision) {
            ExternalScheduleSyncActionIdempotencyDecision.ALREADY_IN_PROGRESS ->
                return result(
                    request = request,
                    status = ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_IN_PROGRESS,
                    eventId = gate.record?.eventId,
                    idempotencyRecord = gate.record,
                    message = "Action is already IN_PROGRESS; execution was skipped",
                    warnings = gate.warnings,
                )

            ExternalScheduleSyncActionIdempotencyDecision.ALREADY_COMPLETED ->
                return result(
                    request = request,
                    status = ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_COMPLETED,
                    eventId = gate.record?.eventId,
                    idempotencyRecord = gate.record,
                    message = "Action is already COMPLETED; execution was skipped",
                    warnings = gate.warnings,
                )

            ExternalScheduleSyncActionIdempotencyDecision.REJECTED ->
                return result(
                    request = request,
                    status = ExternalScheduleSyncActionExecutionStatus.REJECTED,
                    eventId = gate.record?.eventId,
                    idempotencyRecord = gate.record,
                    errors = gate.errors,
                )

            ExternalScheduleSyncActionIdempotencyDecision.STARTED,
            ExternalScheduleSyncActionIdempotencyDecision.RETRY_ALLOWED -> Unit
        }

        val execution = runCatching {
            when (request.action) {
                ExternalScheduleSyncAction.CREATE_EVENT ->
                    requireCurrent(request).let(handler::create)

                ExternalScheduleSyncAction.UPDATE_EVENT ->
                    handler.update(
                        previous = requirePrevious(request),
                        current = requireCurrent(request),
                    )

                ExternalScheduleSyncAction.HANDLE_REMOVED_EVENT ->
                    handler.handleRemoved(
                        previous = requirePrevious(request),
                    )

                ExternalScheduleSyncAction.RECREATE_EVENT_MAPPING ->
                    handler.recreateEventMapping(
                        previous = request.previous,
                        current = request.current,
                    )

                ExternalScheduleSyncAction.NO_OP ->
                    return result(
                        request = request,
                        status = ExternalScheduleSyncActionExecutionStatus.REJECTED,
                        idempotencyRecord = gate.record,
                        errors = listOf("NO_OP must not enter the action executor"),
                    )
            }
        }

        return execution.fold(
            onSuccess = { eventId ->
                val completed = idempotencyService.complete(
                    identity = request.identity,
                    now = now,
                    eventId = eventId,
                )

                if (!completed.successful) {
                    return result(
                        request = request,
                        status = ExternalScheduleSyncActionExecutionStatus.FAILED,
                        eventId = eventId,
                        idempotencyRecord = completed.record,
                        message = "Event action succeeded but idempotency completion failed",
                        warnings = completed.warnings,
                        errors = completed.errors,
                    )
                }

                result(
                    request = request,
                    status = ExternalScheduleSyncActionExecutionStatus.EXECUTED,
                    eventId = eventId ?: completed.record?.eventId,
                    idempotencyRecord = completed.record,
                    warnings = gate.warnings + completed.warnings,
                )
            },
            onFailure = { throwable ->
                val message =
                    throwable.message ?: throwable::class.simpleName ?: "unknown execution failure"

                val failed = idempotencyService.fail(
                    identity = request.identity,
                    now = now,
                    message = message,
                )

                result(
                    request = request,
                    status = ExternalScheduleSyncActionExecutionStatus.FAILED,
                    eventId = failed.record?.eventId,
                    idempotencyRecord = failed.record,
                    message = message,
                    warnings = gate.warnings + failed.warnings,
                    errors = failed.errors,
                )
            },
        )
    }

    private fun requireCurrent(
        request: ExternalScheduleSyncActionExecutionRequest,
    ) = request.current
        ?: error("Action ${request.action} requires current external schedule record")

    private fun requirePrevious(
        request: ExternalScheduleSyncActionExecutionRequest,
    ) = request.previous
        ?: error("Action ${request.action} requires previous external schedule record")

    private fun result(
        request: ExternalScheduleSyncActionExecutionRequest,
        status: ExternalScheduleSyncActionExecutionStatus,
        eventId: String? = null,
        idempotencyRecord: me.moiclathy.dism.external.schedule.sync.idempotency.ExternalScheduleSyncActionExecutionRecord? = null,
        message: String? = null,
        warnings: List<String> = emptyList(),
        errors: List<String> = emptyList(),
    ) = ExternalScheduleSyncActionExecutionResult(
        status = status,
        externalKey = request.externalKey,
        action = request.action.name,
        eventId = eventId,
        idempotencyRecord = idempotencyRecord,
        message = message,
        warnings = warnings,
        errors = errors,
    )
}
