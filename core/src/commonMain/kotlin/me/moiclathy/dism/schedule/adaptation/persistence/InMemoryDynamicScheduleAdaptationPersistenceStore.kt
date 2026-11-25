package me.moiclathy.dism.schedule.adaptation.persistence

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class InMemoryDynamicScheduleAdaptationPersistenceStore :
    DynamicScheduleAdaptationPersistenceStore {

    private val records =
        mutableMapOf<
            DynamicScheduleItemId,
            DynamicScheduleAdaptationPersistenceRecord,
        >()

    override suspend fun save(
        record: DynamicScheduleAdaptationPersistenceRecord,
    ) {
        records[record.itemId] = record
    }

    override suspend fun get(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationPersistenceRecord? {
        return records[itemId]
    }

    override suspend fun delete(
        itemId: DynamicScheduleItemId,
    ) {
        records.remove(itemId)
    }

    override suspend fun clear() {
        records.clear()
    }
}
