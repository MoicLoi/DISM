package me.moiclathy.dism.schedule.history

interface DynamicScheduleDecisionHistoryService {

    suspend fun query(
        query: DynamicScheduleDecisionHistoryQuery,
    ): DynamicScheduleDecisionHistoryResult
}
