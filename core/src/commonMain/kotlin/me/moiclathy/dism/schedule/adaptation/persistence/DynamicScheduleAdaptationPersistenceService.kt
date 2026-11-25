package me.moiclathy.dism.schedule.adaptation.persistence

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

interface DynamicScheduleAdaptationPersistenceService {

    suspend fun persist(
        adaptation: DynamicSchedulePriorityAdaptation,
        humanOverride: DynamicScheduleHumanOverride? = null,
    ): DynamicScheduleAdaptationPersistenceRecord

    suspend fun restore(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationPersistenceRecord?
}
