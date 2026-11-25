package me.moiclathy.dism.schedule.attention

interface DynamicScheduleAttentionExplanationEngine {

    fun explain(
        input: DynamicScheduleAttentionExplanationInput,
        policy: DynamicScheduleAttentionExplanationPolicy =
            DynamicScheduleAttentionExplanationPolicy(),
    ): DynamicScheduleAttentionExplanation
}
