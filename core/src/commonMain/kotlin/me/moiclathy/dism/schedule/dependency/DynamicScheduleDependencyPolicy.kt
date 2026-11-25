package me.moiclathy.dism.schedule.dependency

import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleDependencyPolicy(
    val minimumEffectiveConfidence: Double = 0.05,
    val preserveTerminalStates: Boolean = true,
    val blockOnUnmetDependency: Boolean = true,
    val restoreBlockedWhenDependenciesBecomeSatisfied: Boolean = false,
    val associatedCountsAsBlocking: Boolean = false,
) {
    init {
        require(minimumEffectiveConfidence in 0.0..1.0)
    }
}
