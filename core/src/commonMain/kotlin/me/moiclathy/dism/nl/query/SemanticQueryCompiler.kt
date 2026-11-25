package me.moiclathy.dism.nl.query

import me.moiclathy.dism.nl.SemanticIntent

interface SemanticQueryCompiler {
    fun compile(intent: SemanticIntent): List<SemanticStateQueryCandidate>
}
