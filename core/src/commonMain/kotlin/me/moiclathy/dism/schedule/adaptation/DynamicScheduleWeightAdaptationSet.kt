package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.4
 *
 * 多个 Schedule Item 的动态权重适配结果。
 */
data class DynamicScheduleWeightAdaptationSet(
    val adaptations:
        List<DynamicScheduleWeightAdaptation>,
) {

    init {
        require(
            adaptations
                .map { it.itemId }
                .distinct()
                .size == adaptations.size,
        ) {
            "adaptation item ids must be unique"
        }
    }

    val size: Int
        get() = adaptations.size

    val isEmpty: Boolean
        get() = adaptations.isEmpty()

    fun find(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleWeightAdaptation? =
        adaptations.firstOrNull {
            it.itemId == itemId
        }

    fun enabled(): List<DynamicScheduleWeightAdaptation> =
        adaptations.filter { it.enabled }
}
