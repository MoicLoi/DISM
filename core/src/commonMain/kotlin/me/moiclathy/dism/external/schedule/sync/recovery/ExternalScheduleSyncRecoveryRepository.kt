package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord

/**
 * Recovery 对 Sync History 的读取边界。
 *
 * 不重新定义数据库。
 */
interface ExternalScheduleSyncRecoveryRepository {

    fun findLatestRun(
        scheduleId:
            ExternalScheduleId,
    ): List<ExternalScheduleSyncPersistenceRecord>

    fun findLatest(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,
    ): ExternalScheduleSyncPersistenceRecord?
}
