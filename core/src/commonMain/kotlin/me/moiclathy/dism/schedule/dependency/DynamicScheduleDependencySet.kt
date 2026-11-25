package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleDependencySet(
    val dependencies: List<DynamicScheduleDependency> = emptyList(),
) {
    init {
        require(dependencies.map { it.id }.distinct().size == dependencies.size) {
            "dependency ids must be unique"
        }
        require(dependencies.map {
            Triple(it.fromItemId, it.toItemId, it.type)
        }.distinct().size == dependencies.size) {
            "duplicate dependency relations are not allowed"
        }
    }

    val activeDependencies: List<DynamicScheduleDependency>
        get() = dependencies.filter { it.effective }

    fun outgoing(itemId: DynamicScheduleItemId): List<DynamicScheduleDependency> =
        activeDependencies.filter { it.fromItemId == itemId }

    fun incoming(itemId: DynamicScheduleItemId): List<DynamicScheduleDependency> =
        activeDependencies.filter { it.toItemId == itemId }

    fun add(dependency: DynamicScheduleDependency): DynamicScheduleDependencySet =
        copy(dependencies = dependencies + dependency)

    fun replace(dependency: DynamicScheduleDependency): DynamicScheduleDependencySet {
        require(dependencies.any { it.id == dependency.id }) {
            "dependency does not exist: ${dependency.id}"
        }
        return copy(
            dependencies = dependencies.map {
                if (it.id == dependency.id) dependency else it
            },
        )
    }
}
