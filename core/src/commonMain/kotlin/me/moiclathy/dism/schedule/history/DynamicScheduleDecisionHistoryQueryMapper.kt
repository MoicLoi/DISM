package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery

internal fun DynamicScheduleDecisionHistoryQuery.toSnapshotQuery():
    DynamicScheduleDecisionSnapshotQuery {

    return DynamicScheduleDecisionSnapshotQuery(
        itemId = itemId,
        from = from,
        until = until,
        scheduleRevision = scheduleRevision,
        limit = limit,
    )
}
