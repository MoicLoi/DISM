package me.moiclathy.dism.nl.resolution

import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.nl.SemanticMention

interface EntityResolver {
    fun resolve(mention: SemanticMention): EntityResolutionResult
}
