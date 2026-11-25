package me.moiclathy.dism.e2e.scenario

/**
 * Small deterministic runner for real Core fixtures.
 * It deliberately knows nothing about SemanticStateEngine or Attention internals.
 */
class DeterministicScenarioRunner<S> {
    fun run(initialState: S, steps: List<E2eScenarioStep<S>>): E2eScenarioResult<S> {
        var current = initialState
        val checkpoints = ArrayList<E2eScenarioCheckpoint<S>>(steps.size)

        steps.forEachIndexed { index, step ->
            current = step.run(current)
            checkpoints += E2eScenarioCheckpoint(
                index = index,
                stepName = step.name,
                state = current,
            )
        }

        return E2eScenarioResult(
            initialState = initialState,
            finalState = current,
            checkpoints = checkpoints,
        )
    }
}
