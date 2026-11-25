package me.moiclathy.dism.external.schedule.sync.idempotency

/**
 * 内存实现。
 *
 * 主要用于：
 * - 单元测试
 * - E2E
 * - Runtime prototype
 */
class InMemoryExternalScheduleSyncIdempotencyRepository :
    ExternalScheduleSyncIdempotencyRepository {

    private val records =
        linkedMapOf<
            String,
            ExternalScheduleSyncIdempotencyRecord
        >()

    override fun find(
        key:
            ExternalScheduleSyncIdempotencyKey,
    ):
        ExternalScheduleSyncIdempotencyRecord? {

        return records[
            key.value
        ]
    }

    override fun save(
        record:
            ExternalScheduleSyncIdempotencyRecord,
    ):
        ExternalScheduleSyncIdempotencyRecord {

        records[
            record.key.value
        ] = record

        return record
    }

    override fun delete(
        key:
            ExternalScheduleSyncIdempotencyKey,
    ) {

        records.remove(
            key.value
        )
    }

    override fun findByRun(
        syncRunId:
            String,
    ):
        List<ExternalScheduleSyncIdempotencyRecord> {

        return records
            .values
            .filter {
                it.key.syncRunId == syncRunId
            }
            .sortedBy {
                it.key.externalKey
            }
    }
}
