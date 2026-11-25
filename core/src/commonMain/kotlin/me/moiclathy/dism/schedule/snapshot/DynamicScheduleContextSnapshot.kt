package me.moiclathy.dism.schedule.snapshot

data class DynamicScheduleContextSnapshot(
    val activeContextKeys: Set<String> = emptySet(),
    val requiredSatisfied: Boolean = true,
    val excludedSatisfied: Boolean = true,
) {
    init {
        require(
            activeContextKeys.none { it.isBlank() },
        )
    }
}
