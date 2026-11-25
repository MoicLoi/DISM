package me.moiclathy.dism.nl.query.resolution

import me.moiclathy.dism.nl.query.SemanticStateQueryCandidate

interface SemanticStateQueryResolver {
    fun resolve(candidate: SemanticStateQueryCandidate): SemanticStateQueryResolution
}
