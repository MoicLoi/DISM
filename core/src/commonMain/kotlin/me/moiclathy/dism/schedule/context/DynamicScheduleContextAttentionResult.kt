package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleContextAttentionResult(
    val itemId: DynamicScheduleItemId,

    val baseFactor: Double,

    val contextFactor: Double,

    val effectiveFactor: Double,

    val activeContextKeys: Set<String>,

    val matchedHistoryKeys:
        List<DynamicScheduleContextAttentionKey>,

    val reasons: List<String> = emptyList(),
) {
    init {
        require(baseFactor >= 0.0)
        require(contextFactor >= 0.0)
        require(effectiveFactor >= 0.0)
    }
}
