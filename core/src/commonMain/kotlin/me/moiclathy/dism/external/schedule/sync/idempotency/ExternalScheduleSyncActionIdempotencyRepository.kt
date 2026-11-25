package me.moiclathy.dism.external.schedule.sync.idempotency

interface ExternalScheduleSyncActionIdempotencyRepository {

    fun find(
        identity: ExternalScheduleSyncActionIdentity,
    ): ExternalScheduleSyncActionExecutionRecord?

    fun save(
        record: ExternalScheduleSyncActionExecutionRecord,
    ): ExternalScheduleSyncActionExecutionRecord

    fun delete(
        identity: ExternalScheduleSyncActionIdentity,
    ): Boolean
}
