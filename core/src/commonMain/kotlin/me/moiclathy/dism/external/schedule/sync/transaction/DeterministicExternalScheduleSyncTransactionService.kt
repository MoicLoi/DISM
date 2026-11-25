package me.moiclathy.dism.external.schedule.sync.transaction

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 确定性的 Sync Run Transaction Service。
 */
class DeterministicExternalScheduleSyncTransactionService(
    private val repository:
        ExternalScheduleSyncRunRepository,
) : ExternalScheduleSyncTransactionService {

    override fun begin(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        startedAt:
            Instant,

        totalActions:
            Int,
    ):
        ExternalScheduleSyncRunRecord {

        require(totalActions >= 0) {
            "totalActions must not be negative"
        }

        val existing =
            repository.find(
                runId
            )

        if (existing != null) {
            return existing
        }

        val status =
            if (totalActions == 0) {
                ExternalScheduleSyncRunStatus
                    .NO_CHANGES
            } else {
                ExternalScheduleSyncRunStatus
                    .IN_PROGRESS
            }

        val record =
            ExternalScheduleSyncRunRecord(

                scheduleId =
                    scheduleId,

                runId =
                    runId,

                status =
                    status,

                phase =
                    ExternalScheduleSyncRunPhase
                        .EXECUTING,

                startedAt =
                    startedAt,

                updatedAt =
                    startedAt,

                completedAt =
                    if (totalActions == 0) {
                        startedAt
                    } else {
                        null
                    },

                totalActions =
                    totalActions,

                completedActions =
                    0,

                failedActions =
                    0,
            )

        return repository.save(
            record
        )
    }

    override fun actionCompleted(
        runId:
            ExternalScheduleSyncRunId,

        updatedAt:
            Instant,
    ):
        ExternalScheduleSyncRunRecord? {

        val existing =
            repository.find(
                runId
            )
            ?: return null

        if (
            existing.terminal
        ) {
            return existing
        }

        val completed =
            (
                existing.completedActions + 1
            ).coerceAtMost(
                existing.totalActions
            )

        val newStatus =
            if (
                completed >= existing.totalActions &&
                existing.failedActions == 0
            ) {
                ExternalScheduleSyncRunStatus
                    .COMPLETED
            } else if (
                existing.failedActions > 0
            ) {
                ExternalScheduleSyncRunStatus
                    .PARTIAL
            } else {
                ExternalScheduleSyncRunStatus
                    .IN_PROGRESS
            }

        val updated =
            existing.copy(

                status =
                    newStatus,

                phase =
                    if (
                        newStatus ==
                            ExternalScheduleSyncRunStatus
                                .COMPLETED
                    ) {
                        ExternalScheduleSyncRunPhase
                            .COMPLETED
                    } else {
                        ExternalScheduleSyncRunPhase
                            .EXECUTING
                    },

                updatedAt =
                    updatedAt,

                completedAt =
                    if (
                        newStatus ==
                            ExternalScheduleSyncRunStatus
                                .COMPLETED
                    ) {
                        updatedAt
                    } else {
                        null
                    },

                completedActions =
                    completed,
            )

        return repository.save(
            updated
        )
    }

    override fun actionFailed(
        runId:
            ExternalScheduleSyncRunId,

        updatedAt:
            Instant,

        message:
            String,
    ):
        ExternalScheduleSyncRunRecord? {

        require(message.isNotBlank()) {
            "message must not be blank"
        }

        val existing =
            repository.find(
                runId
            )
            ?: return null

        if (
            existing.terminal
        ) {
            return existing
        }

        val failed =
            (
                existing.failedActions + 1
            ).coerceAtMost(
                existing.totalActions
            )

        val updated =
            existing.copy(

                status =
                    ExternalScheduleSyncRunStatus
                        .PARTIAL,

                phase =
                    ExternalScheduleSyncRunPhase
                        .EXECUTING,

                updatedAt =
                    updatedAt,

                failedActions =
                    failed,

                message =
                    message,
            )

        return repository.save(
            updated
        )
    }

    override fun complete(
        runId:
            ExternalScheduleSyncRunId,

        completedAt:
            Instant,
    ):
        ExternalScheduleSyncRunRecord? {

        val existing =
            repository.find(
                runId
            )
            ?: return null

        if (
            existing.terminal
        ) {
            return existing
        }

        val status =
            if (
                existing.failedActions == 0 &&
                existing.completedActions >=
                    existing.totalActions
            ) {
                ExternalScheduleSyncRunStatus
                    .COMPLETED
            } else if (
                existing.completedActions +
                    existing.failedActions >=
                    existing.totalActions
            ) {
                ExternalScheduleSyncRunStatus
                    .PARTIAL
            } else {
                ExternalScheduleSyncRunStatus
                    .IN_PROGRESS
            }

        val updated =
            existing.copy(

                status =
                    status,

                phase =
                    if (
                        status ==
                            ExternalScheduleSyncRunStatus
                                .COMPLETED
                    ) {
                        ExternalScheduleSyncRunPhase
                            .COMPLETED
                    } else {
                        ExternalScheduleSyncRunPhase
                            .EXECUTING
                    },

                updatedAt =
                    completedAt,

                completedAt =
                    if (
                        status ==
                            ExternalScheduleSyncRunStatus
                                .COMPLETED
                    ) {
                        completedAt
                    } else {
                        null
                    },
            )

        return repository.save(
            updated
        )
    }

    override fun recover(
        runId:
            ExternalScheduleSyncRunId,

        actions:
            List<ExternalScheduleSyncRunAction>,
    ):
        ExternalScheduleSyncRunRecoveryResult {

        val run =
            repository.find(
                runId
            )

        if (run == null) {

            return ExternalScheduleSyncRunRecoveryResult.failure(

                errors =
                    listOf(
                        "Sync Run not found: ${runId.value}"
                    )
            )
        }

        if (
            run.terminal
        ) {

            val plan =
                ExternalScheduleSyncRunRecoveryPlan(

                    scheduleId =
                        run.scheduleId,

                    runId =
                        run.runId,

                    originalRun =
                        run,

                    pendingActions =
                        emptyList(),

                    failedActions =
                        emptyList(),

                    completedActions =
                        actions.filter {
                            it.completed
                        },
                )

            return ExternalScheduleSyncRunRecoveryResult.success(
                plan =
                    plan,

                warnings =
                    listOf(
                        "Sync Run is already terminal"
                    ),
            )
        }

        val completedActions =
            actions.filter {
                it.completed &&
                    !it.failed
            }

        val failedActions =
            actions.filter {
                it.failed
            }

        val pendingActions =
            actions.filter {
                !it.completed &&
                    !it.failed
            }

        val plan =
            ExternalScheduleSyncRunRecoveryPlan(

                scheduleId =
                    run.scheduleId,

                runId =
                    run.runId,

                originalRun =
                    run.copy(
                        status =
                            ExternalScheduleSyncRunStatus
                                .RECOVERING,

                        phase =
                            ExternalScheduleSyncRunPhase
                                .RECOVERING,
                    ),

                pendingActions =
                    pendingActions,

                failedActions =
                    failedActions,

                completedActions =
                    completedActions,
            )

        repository.save(
            plan.originalRun
        )

        return ExternalScheduleSyncRunRecoveryResult.success(
            plan =
                plan
        )
    }
}
