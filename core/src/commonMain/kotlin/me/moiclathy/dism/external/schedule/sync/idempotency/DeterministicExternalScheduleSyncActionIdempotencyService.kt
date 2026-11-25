package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.time.Instant

/**
 * 11.29 action-level idempotency boundary.
 *
 * It gates execution; it does not execute the actual Event action.
 */
class DeterministicExternalScheduleSyncActionIdempotencyService(
    private val repository: ExternalScheduleSyncActionIdempotencyRepository,
) {

    fun begin(
        identity: ExternalScheduleSyncActionIdentity,
        now: Instant,
    ): ExternalScheduleSyncActionIdempotencyResult {
        val existing = repository.find(identity)

        if (existing == null) {
            val created = repository.save(
                ExternalScheduleSyncActionExecutionRecord(
                    identity = identity,
                    status = ExternalScheduleSyncActionExecutionStatus.IN_PROGRESS,
                    startedAt = now,
                    updatedAt = now,
                ),
            )
            return ExternalScheduleSyncActionIdempotencyResult(
                decision = ExternalScheduleSyncActionIdempotencyDecision.STARTED,
                record = created,
            )
        }

        return when (existing.status) {
            ExternalScheduleSyncActionExecutionStatus.NEW ->
                startExisting(existing, now)

            ExternalScheduleSyncActionExecutionStatus.IN_PROGRESS ->
                ExternalScheduleSyncActionIdempotencyResult(
                    decision = ExternalScheduleSyncActionIdempotencyDecision.ALREADY_IN_PROGRESS,
                    record = existing,
                    warnings = listOf(
                        "Action identity is already IN_PROGRESS; execution must not be duplicated",
                    ),
                )

            ExternalScheduleSyncActionExecutionStatus.COMPLETED ->
                ExternalScheduleSyncActionIdempotencyResult(
                    decision = ExternalScheduleSyncActionIdempotencyDecision.ALREADY_COMPLETED,
                    record = existing,
                    warnings = listOf(
                        "Action identity is already COMPLETED; execution must not be repeated",
                    ),
                )

            ExternalScheduleSyncActionExecutionStatus.FAILED -> {
                val retry = repository.save(
                    existing.copy(
                        status = ExternalScheduleSyncActionExecutionStatus.IN_PROGRESS,
                        startedAt = existing.startedAt ?: now,
                        completedAt = null,
                        updatedAt = now,
                        message = null,
                    ),
                )
                ExternalScheduleSyncActionIdempotencyResult(
                    decision = ExternalScheduleSyncActionIdempotencyDecision.RETRY_ALLOWED,
                    record = retry,
                    warnings = listOf(
                        "Previous attempt FAILED; this attempt reopened the same action identity",
                    ),
                )
            }
        }
    }

    fun complete(
        identity: ExternalScheduleSyncActionIdentity,
        now: Instant,
        eventId: String? = null,
        message: String? = null,
    ): ExternalScheduleSyncActionIdempotencyResult {
        val existing = repository.find(identity)
            ?: return rejected(identity, "Cannot complete an unknown action identity")

        if (existing.status == ExternalScheduleSyncActionExecutionStatus.COMPLETED) {
            return ExternalScheduleSyncActionIdempotencyResult(
                decision = ExternalScheduleSyncActionIdempotencyDecision.ALREADY_COMPLETED,
                record = existing,
                warnings = listOf("Completion was already recorded"),
            )
        }

        if (existing.status != ExternalScheduleSyncActionExecutionStatus.IN_PROGRESS) {
            return rejected(
                identity,
                "Cannot complete action in status ${existing.status}",
                existing,
            )
        }

        val completed = repository.save(
            existing.copy(
                status = ExternalScheduleSyncActionExecutionStatus.COMPLETED,
                eventId = eventId ?: existing.eventId,
                completedAt = now,
                updatedAt = now,
                message = message,
            ),
        )

        return ExternalScheduleSyncActionIdempotencyResult(
            decision = ExternalScheduleSyncActionIdempotencyDecision.STARTED,
            record = completed,
        )
    }

    fun fail(
        identity: ExternalScheduleSyncActionIdentity,
        now: Instant,
        message: String,
    ): ExternalScheduleSyncActionIdempotencyResult {
        if (message.isBlank()) {
            return rejected(identity, "Failure message must not be blank")
        }

        val existing = repository.find(identity)
            ?: return rejected(identity, "Cannot fail an unknown action identity")

        if (existing.status == ExternalScheduleSyncActionExecutionStatus.COMPLETED) {
            return ExternalScheduleSyncActionIdempotencyResult(
                decision = ExternalScheduleSyncActionIdempotencyDecision.ALREADY_COMPLETED,
                record = existing,
                warnings = listOf(
                    "Failure was ignored because the action is already COMPLETED",
                ),
            )
        }

        val failed = repository.save(
            existing.copy(
                status = ExternalScheduleSyncActionExecutionStatus.FAILED,
                updatedAt = now,
                message = message,
            ),
        )

        return ExternalScheduleSyncActionIdempotencyResult(
            decision = ExternalScheduleSyncActionIdempotencyDecision.RETRY_ALLOWED,
            record = failed,
            warnings = listOf("Action is FAILED and may be retried with the same identity"),
        )
    }

    private fun startExisting(
        existing: ExternalScheduleSyncActionExecutionRecord,
        now: Instant,
    ): ExternalScheduleSyncActionIdempotencyResult {
        val started = repository.save(
            existing.copy(
                status = ExternalScheduleSyncActionExecutionStatus.IN_PROGRESS,
                startedAt = existing.startedAt ?: now,
                updatedAt = now,
                message = null,
            ),
        )

        return ExternalScheduleSyncActionIdempotencyResult(
            decision = ExternalScheduleSyncActionIdempotencyDecision.STARTED,
            record = started,
        )
    }

    private fun rejected(
        identity: ExternalScheduleSyncActionIdentity,
        message: String,
        record: ExternalScheduleSyncActionExecutionRecord? = repository.find(identity),
    ) = ExternalScheduleSyncActionIdempotencyResult(
        decision = ExternalScheduleSyncActionIdempotencyDecision.REJECTED,
        record = record,
        errors = listOf(message),
    )
}
