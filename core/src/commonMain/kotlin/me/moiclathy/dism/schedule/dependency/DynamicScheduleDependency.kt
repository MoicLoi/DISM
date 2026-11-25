package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import kotlinx.serialization.Serializable

/**
 * Directed relationship between two schedule items.
 *
 * fromItemId is the prerequisite/source and toItemId is the dependent/target.
 */
@Serializable
data class DynamicScheduleDependency(
    val id: DynamicScheduleDependencyId,
    val fromItemId: DynamicScheduleItemId,
    val toItemId: DynamicScheduleItemId,
    val type: DynamicScheduleDependencyType,
    val source: DynamicScheduleDependencySource = DynamicScheduleDependencySource.DERIVED,
    val confidence: Double = 1.0,
    val active: Boolean = true,
) {
    init {
        require(fromItemId != toItemId) { "dependency must not point to itself" }
        require(confidence in 0.0..1.0) { "confidence must be in [0, 1]" }
    }

    val effective: Boolean
        get() = active && confidence > 0.0
}
