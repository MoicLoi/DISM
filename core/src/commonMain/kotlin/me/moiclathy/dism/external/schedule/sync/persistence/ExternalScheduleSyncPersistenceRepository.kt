package me.moiclathy.dism.external.schedule.sync.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * External Schedule Sync 历史仓储。
 */
interface ExternalScheduleSyncPersistenceRepository {

    /**
     * 保存一条同步执行记录。
     */
    fun save(
        record:
            ExternalScheduleSyncPersistenceRecord,
    )

    /**
     * 批量保存。
     */
    fun saveAll(
        records:
            List<ExternalScheduleSyncPersistenceRecord>,
    )

    /**
     * 查询某个 Schedule 的全部同步记录。
     */
    fun findBySchedule(
        scheduleId:
            ExternalScheduleId,
    ): List<ExternalScheduleSyncPersistenceRecord>

    /**
     * 查询指定 Sync Run。
     */
    fun findByRun(
        syncRunId:
            String,
    ): List<ExternalScheduleSyncPersistenceRecord>

    /**
     * 查询某个外部 Key 的最近同步记录。
     */
    fun findLatest(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,
    ): ExternalScheduleSyncPersistenceRecord?

    /**
     * 查询最近一次同步批次。
     */
    fun findLatestRun(
        scheduleId:
            ExternalScheduleId,
    ): String?

    /**
     * 删除指定 Sync Run。
     *
     * 默认实现通常不会主动使用。
     */
    fun deleteRun(
        syncRunId:
            String,
    )
}
