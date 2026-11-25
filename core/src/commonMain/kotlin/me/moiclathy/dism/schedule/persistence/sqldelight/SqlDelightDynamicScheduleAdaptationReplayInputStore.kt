package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlin.time.Clock

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.ScheduleAdaptationReplayInputQueries

import me.moiclathy.dism.schedule.adaptation.replay
    .DynamicScheduleAdaptationReplayInput
import me.moiclathy.dism.schedule.adaptation.replay
    .DynamicScheduleAdaptationReplayInputPayloadCodec
import me.moiclathy.dism.schedule.adaptation.replay
    .DynamicScheduleAdaptationReplayInputStore
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.12
 *
 * SQLDelight 版的 Replay Input 存储。
 *
 * ### 关于 persisted_at_epoch_ms
 *
 * 端口上的 `save(itemId, input)` 不带时间戳，
 * 但表里有一列。这里用可注入的 [clock] 填，
 * 与 14.11 的做法一致：不在内部偷偷读系统时间。
 */
class SqlDelightDynamicScheduleAdaptationReplayInputStore(
    private val queries: ScheduleAdaptationReplayInputQueries,

    private val clock: Clock = Clock.System,
) : DynamicScheduleAdaptationReplayInputStore {

    constructor(
        database: DismDatabase,
        clock: Clock = Clock.System,
    ) : this(
        queries = database.scheduleAdaptationReplayInputQueries,
        clock = clock,
    )

    override suspend fun save(
        itemId: DynamicScheduleItemId,
        input: DynamicScheduleAdaptationReplayInput,
    ) {
        queries.insertOrReplace(
            item_id = itemId.value,

            payload_json =
                DynamicScheduleAdaptationReplayInputPayloadCodec
                    .encode(input),

            persisted_at_epoch_ms =
                clock.now().toEpochMilliseconds(),

            schema_version =
                DynamicScheduleAdaptationReplayInputPayloadCodec
                    .VERSION,
        )
    }

    override suspend fun get(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationReplayInput? {

        val row =
            queries
                .selectByItemId(itemId.value)
                .executeAsOneOrNull()
                ?: return null

        return DynamicScheduleAdaptationReplayInputPayloadCodec
            .decode(row.payload_json)
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
