package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlin.time.Instant

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.ScheduleAdaptationQueries

import me.moiclathy.dism.schedule.adaptation.persistence
    .DynamicScheduleAdaptationPersistenceRecord
import me.moiclathy.dism.schedule.adaptation.persistence
    .DynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.adaptation.persistence
    .DynamicScheduleAdaptationPersistencePayload
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.11
 *
 * SQLDelight 版的 Adaptation 持久化。
 *
 * 一行 = 一次 JSON，见
 * [DynamicScheduleAdaptationPersistencePayload] 的说明。
 *
 * ### 为什么读取时要校验 schemaVersion
 *
 * 表里有 `schema_version`，payload 里也有一个。
 * 两者不一致说明这行数据被外部改过，
 * 继续解析可能会静默产出错误的适配结果。
 */
class SqlDelightDynamicScheduleAdaptationPersistenceStore(
    private val queries: ScheduleAdaptationQueries,
) : DynamicScheduleAdaptationPersistenceStore {

    constructor(
        database: DismDatabase,
    ) : this(
        queries = database.scheduleAdaptationQueries,
    )

    override suspend fun save(
        record: DynamicScheduleAdaptationPersistenceRecord,
    ) {

        val payload =
            DynamicScheduleAdaptationPersistencePayload(
                adaptation = record.adaptation,
                humanOverride = record.humanOverride,
                schemaVersion = record.schemaVersion,
            )

        queries.insertOrReplace(
            item_id =
                record.itemId.value,

            payload_json =
                DynamicScheduleAdaptationSqlDelightCodec
                    .encode(payload),

            persisted_at_epoch_ms =
                record.persistedAt.toEpochMilliseconds(),

            schema_version =
                record.schemaVersion,
        )
    }

    override suspend fun get(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationPersistenceRecord? {

        val row =
            queries
                .selectByItemId(itemId.value)
                .executeAsOneOrNull()
                ?: return null

        val payload =
            DynamicScheduleAdaptationSqlDelightCodec
                .decode(row.payload_json)

        require(
            payload.schemaVersion == row.schema_version,
        ) {
            "Adaptation persistence schema mismatch: " +
                "payload=${payload.schemaVersion}, " +
                "row=${row.schema_version}"
        }

        return DynamicScheduleAdaptationPersistenceRecord(
            itemId =
                DynamicScheduleItemId(row.item_id),

            adaptation =
                payload.adaptation,

            humanOverride =
                payload.humanOverride,

            persistedAt =
                Instant.fromEpochMilliseconds(
                    row.persisted_at_epoch_ms,
                ),

            schemaVersion =
                row.schema_version,
        )
    }

    override suspend fun delete(
        itemId: DynamicScheduleItemId,
    ) {
        queries.deleteByItemId(itemId.value)
    }

    override suspend fun clear() {
        queries.deleteAll()
    }
}
