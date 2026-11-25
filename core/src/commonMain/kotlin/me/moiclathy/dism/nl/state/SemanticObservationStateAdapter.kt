package me.moiclathy.dism.nl.state

import me.moiclathy.dism.nl.observation.SemanticObservationCompilation

/** Boundary between NLP compilation and the real SemanticStateEngine. */
interface SemanticObservationStateAdapter<STATE, RESULT> {
    fun apply(compilation: SemanticObservationCompilation, state: STATE): RESULT
}
