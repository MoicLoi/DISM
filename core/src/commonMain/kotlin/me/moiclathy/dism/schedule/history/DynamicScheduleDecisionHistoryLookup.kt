package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleDecisionHistoryLookup(
    private val history:
        DynamicScheduleDecisionHistoryService,
) {

    suspend fun latest(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionHistoryEntry? {

        return history
            .query(
                DynamicScheduleDecisionHistoryQuery(
                    itemId = itemId,
                    limit = 1,
                ),
            )
            .entries
            .firstOrNull()
    }

    suspend fun recent(
        itemId: DynamicScheduleItemId,
        limit: Int = 10,
    ): List<DynamicScheduleDecisionHistoryEntry> {

        return history
            .query(
                DynamicScheduleDecisionHistoryQuery(
                    itemId = itemId,
                    limit = limit,
                ),
            )
            .entries
    }
}
