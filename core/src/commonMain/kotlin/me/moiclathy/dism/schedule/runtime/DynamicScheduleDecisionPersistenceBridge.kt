package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

interface DynamicScheduleDecisionPersistenceBridge {

    suspend fun persist(
        input:
            DynamicScheduleDecisionPersistenceInput,
    ): DynamicScheduleDecisionPersistenceResult
}
