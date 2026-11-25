package me.moiclathy.dism.schedule.snapshot

data class DynamicScheduleDependencySnapshot(
    val requiredSatisfied: Boolean,

    val missingDependencyIds: List<String> = emptyList(),

    val requiredDependencyCount: Int = 0,
) {
    init {
        require(requiredDependencyCount >= 0)

        require(
            missingDependencyIds
                .none { it.isBlank() },
        )

        require(
            missingDependencyIds.size <=
                requiredDependencyCount,
        )
    }
}
