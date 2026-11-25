package me.moiclathy.dism.schedule.adaptation.replay

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class InMemoryDynamicScheduleAdaptationReplayInputStore :
    DynamicScheduleAdaptationReplayInputStore {

    private val inputs =
        mutableMapOf<
            DynamicScheduleItemId,
            DynamicScheduleAdaptationReplayInput,
        >()

    override suspend fun save(
        itemId: DynamicScheduleItemId,
        input: DynamicScheduleAdaptationReplayInput,
    ) {
        inputs[itemId] = input
    }

    override suspend fun get(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationReplayInput? =
        inputs[itemId]

    override suspend fun delete(
        itemId: DynamicScheduleItemId,
    ) {
        inputs.remove(itemId)
    }

    override suspend fun clear() {
        inputs.clear()
    }
}
