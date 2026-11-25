package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.attention.adaptation.AttentionFeedback
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleAttentionFeedback(
    val itemId: DynamicScheduleItemId,
    val feedback: AttentionFeedback,
)
