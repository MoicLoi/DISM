package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleAttentionExplanation(
    val itemId: DynamicScheduleItemId,

    val summary: String,

    val reasons:
        List<DynamicScheduleAttentionExplanationReason>,

    val resolvedPriority: Double,

    val attentionRequired: Boolean,
) {
    val decisiveReasons:
        List<DynamicScheduleAttentionExplanationReason>
        get() = reasons.filter { it.decisive }
}
