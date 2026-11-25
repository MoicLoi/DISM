package me.moiclathy.dism.external.schedule.sync

/**
 * 一次 Sync Runtime 的完整结果。
 */
data class ExternalScheduleSyncRuntimeResult(
    val status: ExternalScheduleSyncRuntimeStatus,

    val plan: ExternalScheduleSyncPlan,

    val executions:
        List<ExternalScheduleSyncExecutionRecord>,

    val warnings: List<String> = emptyList(),

    val errors: List<String> = emptyList(),
) {

    val successfulCount: Int
        get() =
            executions.count {
                it.successful
            }

    val failedCount: Int
        get() =
            executions.count {
                !it.successful
            }

    val createdCount: Int
        get() =
            executions.count {
                it.status ==
                    ExternalScheduleSyncExecutionStatus.CREATED
            }

    val updatedCount: Int
        get() =
            executions.count {
                it.status ==
                    ExternalScheduleSyncExecutionStatus.UPDATED
            }

    val removedCount: Int
        get() =
            executions.count {
                it.status ==
                    ExternalScheduleSyncExecutionStatus.REMOVED
            }

    val skippedCount: Int
        get() =
            executions.count {
                it.status ==
                    ExternalScheduleSyncExecutionStatus.SKIPPED
            }

    val hasFailures: Boolean
        get() =
            failedCount > 0
}
