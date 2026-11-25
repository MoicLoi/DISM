package me.moiclathy.dism.e2e.scenario

/** Immutable result of a deterministic scenario run. */
data class E2eScenarioResult<S>(
    val initialState: S,
    val finalState: S,
    val checkpoints: List<E2eScenarioCheckpoint<S>>,
)

data class E2eScenarioCheckpoint<S>(
    val index: Int,
    val stepName: String,
    val state: S,
)
