package me.moiclathy.dism.external.schedule.sync.idempotency

class InMemoryExternalScheduleSyncActionIdempotencyRepository :
    ExternalScheduleSyncActionIdempotencyRepository {

    private val records = linkedMapOf<String, ExternalScheduleSyncActionExecutionRecord>()

    override fun find(
        identity: ExternalScheduleSyncActionIdentity,
    ): ExternalScheduleSyncActionExecutionRecord? =
        records[identity.value]

    override fun save(
        record: ExternalScheduleSyncActionExecutionRecord,
    ): ExternalScheduleSyncActionExecutionRecord {
        records[record.identity.value] = record
        return record
    }

    override fun delete(
        identity: ExternalScheduleSyncActionIdentity,
    ): Boolean =
        records.remove(identity.value) != null
}
