package me.moiclathy.dism.external.schedule.sync.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 内存版 Sync Persistence。
 *
 * 用于：
 *
 * - 单元测试
 * - Core 测试
 * - Runtime 原型
 */
class InMemoryExternalScheduleSyncPersistenceRepository :
    ExternalScheduleSyncPersistenceRepository {

    private val records =
        mutableListOf<
            ExternalScheduleSyncPersistenceRecord
        >()

    override fun save(
        record:
            ExternalScheduleSyncPersistenceRecord,
    ) {

        records += record
    }

    override fun saveAll(
        records:
            List<ExternalScheduleSyncPersistenceRecord>,
    ) {

        this.records += records
    }

    override fun findBySchedule(
        scheduleId:
            ExternalScheduleId,
    ): List<ExternalScheduleSyncPersistenceRecord> {

        return records
            .filter {
                it.scheduleId == scheduleId
            }
            .sortedBy {
                it.executedAt
            }
    }

    override fun findByRun(
        syncRunId:
            String,
    ): List<ExternalScheduleSyncPersistenceRecord> {

        return records
            .filter {
                it.syncRunId == syncRunId
            }
            .sortedBy {
                it.executedAt
            }
    }

    override fun findLatest(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,
    ): ExternalScheduleSyncPersistenceRecord? {

        return records
            .filter {
                it.scheduleId == scheduleId &&
                    it.externalKey == externalKey
            }
            .maxByOrNull {
                it.executedAt
            }
    }

    override fun findLatestRun(
        scheduleId:
            ExternalScheduleId,
    ): String? {

        return records
            .asSequence()
            .filter {
                it.scheduleId == scheduleId
            }
            .maxByOrNull {
                it.executedAt
            }
            ?.syncRunId
    }

    override fun deleteRun(
        syncRunId:
            String,
    ) {

        records.removeAll {
            it.syncRunId == syncRunId
        }
    }
}
