package me.moiclathy.dism.external.schedule.sync.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一次完整 Sync Run 的持久化快照。
 */
data class ExternalScheduleSyncPersistenceSnapshot(

    val scheduleId:
        ExternalScheduleId,

    val syncRunId:
        String,

    val startedAt:
        Instant,

    val completedAt:
        Instant?,

    val records:
        List<ExternalScheduleSyncPersistenceRecord>,
) {

    init {

        require(
            syncRunId.isNotBlank()
        ) {
            "syncRunId must not be blank"
        }

        require(
            completedAt == null ||
                completedAt >= startedAt
        ) {
            "completedAt must not be before startedAt"
        }

        require(
            records.all {
                it.scheduleId == scheduleId &&
                    it.syncRunId == syncRunId
            }
        ) {
            "all records must belong to the same sync run"
        }
    }

    val successfulCount: Int
        get() =
            records.count {
                it.successful
            }

    val failedCount: Int
        get() =
            records.count {
                !it.successful
            }

    val hasFailures: Boolean
        get() =
            failedCount > 0
}
