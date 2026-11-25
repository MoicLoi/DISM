package me.moiclathy.dism.nl.state

import me.moiclathy.dism.nl.observation.SemanticObservationCompilation

data class SemanticStateApplication(
    val compilation: SemanticObservationCompilation,
    val status: StateApplicationStatus,
    val reason: String? = null,
)
