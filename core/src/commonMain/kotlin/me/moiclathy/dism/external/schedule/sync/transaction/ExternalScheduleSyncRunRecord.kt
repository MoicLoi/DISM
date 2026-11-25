package me.moiclathy.dism.external.schedule.sync.transaction

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一次完整 Sync Run 的事务级状态。
 *
 * 注意：
 *
 * 这不是单条 Sync Action。
 *
 * 它描述的是：
 *
 * 一个完整的
 * External Schedule -> Event
 * 同步过程。
 */
data class ExternalScheduleSyncRunRecord(

    val scheduleId:
        ExternalScheduleId,

    val runId:
        ExternalScheduleSyncRunId,

    val status:
        ExternalScheduleSyncRunStatus,

    val phase:
        ExternalScheduleSyncRunPhase,

    val startedAt:
        Instant,

    val updatedAt:
        Instant,

    val completedAt:
        Instant? = null,

    val totalActions:
        Int = 0,

    val completedActions:
        Int = 0,

    val failedActions:
        Int = 0,

    val message:
        String? = null,
) {

    init {

        require(totalActions >= 0) {
            "totalActions must not be negative"
        }

        require(completedActions >= 0) {
            "completedActions must not be negative"
        }

        require(failedActions >= 0) {
            "failedActions must not be negative"
        }

        require(completedActions <= totalActions) {
            "completedActions cannot exceed totalActions"
        }

        require(failedActions <= totalActions) {
            "failedActions cannot exceed totalActions"
        }

        require(
            completedActions + failedActions <= totalActions
        ) {
            "completedActions + failedActions cannot exceed totalActions"
        }

        if (
            status ==
                ExternalScheduleSyncRunStatus.COMPLETED ||
            status ==
                ExternalScheduleSyncRunStatus.RECOVERED
        ) {
            require(completedAt != null) {
                "completedAt is required for terminal successful status"
            }
        }
    }

    val terminal: Boolean
        get() =
            status ==
                ExternalScheduleSyncRunStatus.COMPLETED ||
            status ==
                ExternalScheduleSyncRunStatus.RECOVERED ||
            status ==
                ExternalScheduleSyncRunStatus.NO_CHANGES
}
