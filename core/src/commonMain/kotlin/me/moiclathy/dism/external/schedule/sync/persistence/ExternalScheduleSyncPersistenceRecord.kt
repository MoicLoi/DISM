package me.moiclathy.dism.external.schedule.sync.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus

/**
 * 一条 External Schedule Sync 历史记录。
 *
 * 注意：
 *
 * ExternalSchedulePersistenceRecord
 * 保存的是“当前外部状态”。
 *
 * 本 Record
 * 保存的是“同步执行事实”。
 */
data class ExternalScheduleSyncPersistenceRecord(

    /**
     * 所属 External Schedule。
     */
    val scheduleId:
        ExternalScheduleId,

    /**
     * 外部记录稳定 Key。
     */
    val externalKey:
        String,

    /**
     * 本次执行的动作。
     */
    val action:
        ExternalScheduleSyncAction,

    /**
     * 实际执行结果。
     */
    val status:
        ExternalScheduleSyncExecutionStatus,

    /**
     * 关联的内部 Event ID。
     */
    val eventId:
        String? = null,

    /**
     * 执行时间。
     */
    val executedAt:
        Instant,

    /**
     * 错误或说明。
     */
    val message:
        String? = null,

    /**
     * 本次同步批次 ID。
     */
    val syncRunId:
        String,
) {

    init {

        require(
            externalKey.isNotBlank()
        ) {
            "externalKey must not be blank"
        }

        require(
            syncRunId.isNotBlank()
        ) {
            "syncRunId must not be blank"
        }

        if (
            status ==
                ExternalScheduleSyncExecutionStatus.FAILED
        ) {

            require(
                !message.isNullOrBlank()
            ) {
                "failed record must contain message"
            }
        }
    }

    val successful: Boolean
        get() =
            status !=
                ExternalScheduleSyncExecutionStatus.FAILED
}
