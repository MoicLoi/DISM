package me.moiclathy.dism.nl.resolution

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.nl.SemanticMention
import kotlin.uuid.Uuid

data class EntityResolutionResult(
    val mention: SemanticMention,
    val status: EntityResolutionStatus,
    val entityId: Uuid? = null,
    val candidates: List<EntityResolutionCandidate> = emptyList(),
) {
    val entity: Entity?
        get() = candidates.firstOrNull { it.entity.id == entityId }?.entity
}
