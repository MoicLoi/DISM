package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.domain.entity.Entity

/**
 * Entity Resolution 的一个候选。
 *
 * Resolution 本质上不是一次简单 Map lookup，
 * 而是候选排序。同一个 mention 可能匹配多个 Entity。
 */
data class EntityResolutionCandidate(
    val entity: Entity,
    val score: ResolutionScore,
    val reasons: List<String> = emptyList(),
)
