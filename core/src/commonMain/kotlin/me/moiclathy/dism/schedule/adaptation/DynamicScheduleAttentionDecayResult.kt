package me.moiclathy.dism.schedule.adaptation

data class DynamicScheduleAttentionDecayResult(
    val histories:
        Map<String, DynamicScheduleDecayedAttentionHistory> =
        emptyMap(),

    val diagnostics: List<String> = emptyList(),
) {
    val size: Int
        get() = histories.size
}
