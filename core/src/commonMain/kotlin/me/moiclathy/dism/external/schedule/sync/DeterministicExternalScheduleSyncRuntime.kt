package me.moiclathy.dism.external.schedule.sync

/**
 * 确定性的 External Schedule Sync Runtime。
 *
 * 特点：
 *
 * 1. 按 Plan 顺序执行
 * 2. NO_OP 不调用 Handler
 * 3. 单条失败不会阻止后续记录
 * 4. 最终统一计算 RuntimeStatus
 * 5. 不负责 Persistence
 * 6. 不负责 Event 本身的数据结构
 */
class DeterministicExternalScheduleSyncRuntime(
    private val handler:
        ExternalScheduleSyncHandler,
) : ExternalScheduleSyncRuntime {

    override fun execute(
        plan: ExternalScheduleSyncPlan,
    ): ExternalScheduleSyncRuntimeResult {

        if (plan.changes.isEmpty()) {

            return ExternalScheduleSyncRuntimeResult(
                status =
                    ExternalScheduleSyncRuntimeStatus
                        .NO_CHANGES,

                plan =
                    plan,

                executions =
                    emptyList(),
            )
        }

        val executions =
            mutableListOf<
                ExternalScheduleSyncExecutionRecord
            >()

        val warnings =
            mutableListOf<String>()

        val errors =
            mutableListOf<String>()

        for (change in plan.changes) {

            when (change.action) {

                ExternalScheduleSyncAction.CREATE_EVENT -> {

                    executeCreate(
                        change = change,
                        executions = executions,
                        errors = errors,
                    )
                }

                ExternalScheduleSyncAction.UPDATE_EVENT -> {

                    executeUpdate(
                        change = change,
                        executions = executions,
                        errors = errors,
                    )
                }

                ExternalScheduleSyncAction.HANDLE_REMOVED_EVENT -> {

                    executeRemoval(
                        change = change,
                        executions = executions,
                        errors = errors,
                    )
                }

                ExternalScheduleSyncAction.RECREATE_EVENT_MAPPING -> {
                    val message =
                        "RECREATE_EVENT_MAPPING belongs to the 11.30 execution boundary; " +
                            "the legacy sync runtime does not execute it"

                    warnings += message

                    executions +=
                        ExternalScheduleSyncExecutionRecord(
                            externalKey = change.externalKey,
                            action = change.action,
                            status = ExternalScheduleSyncExecutionStatus.SKIPPED,
                            eventId = change.diff.current?.eventId ?: change.diff.previous?.eventId,
                            message = message,
                        )
                }

                ExternalScheduleSyncAction.NO_OP -> {

                    executions +=
                        ExternalScheduleSyncExecutionRecord(
                            externalKey =
                                change.externalKey,

                            action =
                                change.action,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .SKIPPED,

                            eventId =
                                change.diff
                                    .current
                                    ?.eventId
                                    ?: change.diff
                                        .previous
                                        ?.eventId,

                            message =
                                "No external change detected",
                        )
                }
            }
        }

        val status =
            determineStatus(
                executions = executions,
                totalChanges = plan.changes.size,
            )

        return ExternalScheduleSyncRuntimeResult(
            status =
                status,

            plan =
                plan,

            executions =
                executions,

            warnings =
                warnings,

            errors =
                errors,
        )
    }

    private fun executeCreate(
        change: ExternalScheduleSyncChange,

        executions:
            MutableList<
                ExternalScheduleSyncExecutionRecord
            >,

        errors:
            MutableList<String>,
    ) {

        try {

            val eventId =
                handler.create(
                    change
                )

            if (eventId.isNullOrBlank()) {

                val message =
                    "Create handler returned no eventId " +
                        "for ${change.externalKey}"

                errors += message

                executions +=
                    ExternalScheduleSyncExecutionRecord(
                        externalKey =
                            change.externalKey,

                        action =
                            change.action,

                        status =
                            ExternalScheduleSyncExecutionStatus
                                .FAILED,

                        message =
                            message,
                    )

                return
            }

            executions +=
                ExternalScheduleSyncExecutionRecord(
                    externalKey =
                        change.externalKey,

                    action =
                        change.action,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .CREATED,

                    eventId =
                        eventId,
                )

        } catch (e: Exception) {

            val message =
                "Create failed for " +
                    "${change.externalKey}: " +
                    "${e.message ?: e::class.simpleName}"

            errors += message

            executions +=
                ExternalScheduleSyncExecutionRecord(
                    externalKey =
                        change.externalKey,

                    action =
                        change.action,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .FAILED,

                    message =
                        message,
                )
        }
    }

    private fun executeUpdate(
        change: ExternalScheduleSyncChange,

        executions:
            MutableList<
                ExternalScheduleSyncExecutionRecord
            >,

        errors:
            MutableList<String>,
    ) {

        try {

            val eventId =
                handler.update(
                    change
                )

            if (eventId.isNullOrBlank()) {

                val message =
                    "Update handler returned no eventId " +
                        "for ${change.externalKey}"

                errors += message

                executions +=
                    ExternalScheduleSyncExecutionRecord(
                        externalKey =
                            change.externalKey,

                        action =
                            change.action,

                        status =
                            ExternalScheduleSyncExecutionStatus
                                .FAILED,

                        message =
                            message,
                    )

                return
            }

            executions +=
                ExternalScheduleSyncExecutionRecord(
                    externalKey =
                        change.externalKey,

                    action =
                        change.action,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .UPDATED,

                    eventId =
                        eventId,
                )

        } catch (e: Exception) {

            val message =
                "Update failed for " +
                    "${change.externalKey}: " +
                    "${e.message ?: e::class.simpleName}"

            errors += message

            executions +=
                ExternalScheduleSyncExecutionRecord(
                    externalKey =
                        change.externalKey,

                    action =
                        change.action,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .FAILED,

                    message =
                        message,
                )
        }
    }

    private fun executeRemoval(
        change: ExternalScheduleSyncChange,

        executions:
            MutableList<
                ExternalScheduleSyncExecutionRecord
            >,

        errors:
            MutableList<String>,
    ) {

        try {

            val eventId =
                handler.handleRemoved(
                    change
                )

            executions +=
                ExternalScheduleSyncExecutionRecord(
                    externalKey =
                        change.externalKey,

                    action =
                        change.action,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .REMOVED,

                    eventId =
                        eventId,
                )

        } catch (e: Exception) {

            val message =
                "Removal handling failed for " +
                    "${change.externalKey}: " +
                    "${e.message ?: e::class.simpleName}"

            errors += message

            executions +=
                ExternalScheduleSyncExecutionRecord(
                    externalKey =
                        change.externalKey,

                    action =
                        change.action,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .FAILED,

                    message =
                        message,
                )
        }
    }

    private fun determineStatus(
        executions:
            List<
                ExternalScheduleSyncExecutionRecord
            >,

        totalChanges: Int,
    ): ExternalScheduleSyncRuntimeStatus {

        if (totalChanges == 0) {

            return ExternalScheduleSyncRuntimeStatus
                .NO_CHANGES
        }

        val successful =
            executions.count {
                it.successful
            }

        val failed =
            executions.count {
                !it.successful
            }

        return when {

            failed == 0 ->
                ExternalScheduleSyncRuntimeStatus
                    .COMPLETED

            successful == 0 ->
                ExternalScheduleSyncRuntimeStatus
                    .FAILED

            else ->
                ExternalScheduleSyncRuntimeStatus
                    .PARTIAL
        }
    }
}
