package me.moiclathy.dism.schedule.adaptation

data class DynamicSchedulePriorityAdaptationResult(
    val adaptations:
        List<DynamicSchedulePriorityAdaptation>,

    val diagnostics: List<String> = emptyList(),
) {
    val size: Int
        get() = adaptations.size
}
