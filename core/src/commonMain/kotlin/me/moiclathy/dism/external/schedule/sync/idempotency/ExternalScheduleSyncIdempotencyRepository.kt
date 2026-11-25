package me.moiclathy.dism.external.schedule.sync.idempotency

/**
 * 幂等状态持久化边界。
 *
 * 这里故意不绑定 SQLDelight，
 * 避免重新定义数据库。
 */
interface ExternalScheduleSyncIdempotencyRepository {

    fun find(
        key:
            ExternalScheduleSyncIdempotencyKey,
    ):
        ExternalScheduleSyncIdempotencyRecord?

    fun save(
        record:
            ExternalScheduleSyncIdempotencyRecord,
    ):
        ExternalScheduleSyncIdempotencyRecord

    fun delete(
        key:
            ExternalScheduleSyncIdempotencyKey,
    )

    fun findByRun(
        syncRunId:
            String,
    ):
        List<ExternalScheduleSyncIdempotencyRecord>
}
