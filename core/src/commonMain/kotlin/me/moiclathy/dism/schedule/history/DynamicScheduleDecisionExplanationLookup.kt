package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleDecisionExplanationLookup(
    private val history:
        DynamicScheduleDecisionHistoryService,
) {

    suspend fun latestDecision(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionHistoryEntry? {

        return history
            .query(
                DynamicScheduleDecisionHistoryQuery(
                    itemId = itemId,
                    limit = 1,
                    decisiveOnly = true,
                ),
            )
            .entries
            .firstOrNull()
            ?: history
                .query(
                    DynamicScheduleDecisionHistoryQuery(
                        itemId = itemId,
                        limit = 1,
                    ),
                )
                .entries
                .firstOrNull()
    }
}
