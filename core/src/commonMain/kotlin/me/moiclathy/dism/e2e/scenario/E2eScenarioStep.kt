package me.moiclathy.dism.e2e.scenario

/** A deterministic named step in an end-to-end scenario. */
data class E2eScenarioStep<S>(
    val name: String,
    val run: (S) -> S,
)
