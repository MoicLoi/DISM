package me.moiclathy.dism.external.schedule.sync.transaction

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 内存 Run Repository。
 *
 * 用于：
 * - 单元测试
 * - E2E
 * - Runtime prototype
 */
class InMemoryExternalScheduleSyncRunRepository :
    ExternalScheduleSyncRunRepository {

    private val records =
        linkedMapOf<
            String,
            ExternalScheduleSyncRunRecord
        >()

    override fun find(
        runId:
            ExternalScheduleSyncRunId,
    ):
        ExternalScheduleSyncRunRecord? {

        return records[
            runId.value
        ]
    }

    override fun findLatest(
        scheduleId:
            ExternalScheduleId,
    ):
        ExternalScheduleSyncRunRecord? {

        return records
            .values
            .filter {
                it.scheduleId == scheduleId
            }
            .maxByOrNull {
                it.updatedAt
            }
    }

    override fun save(
        record:
            ExternalScheduleSyncRunRecord,
    ):
        ExternalScheduleSyncRunRecord {

        records[
            record.runId.value
        ] = record

        return record
    }

    override fun delete(
        runId:
            ExternalScheduleSyncRunId,
    ) {

        records.remove(
            runId.value
        )
    }
}
