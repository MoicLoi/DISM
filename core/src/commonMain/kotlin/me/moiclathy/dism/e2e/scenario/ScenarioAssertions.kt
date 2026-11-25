package me.moiclathy.dism.e2e.scenario

/** Minimal assertion helpers usable from commonTest without adding a test framework dependency. */
object ScenarioAssertions {
    fun <S> checkpointNames(result: E2eScenarioResult<S>): List<String> =
        result.checkpoints.map { it.stepName }

    fun <S> checkpointAt(result: E2eScenarioResult<S>, index: Int): S =
        result.checkpoints.getOrNull(index)?.state
            ?: error("No scenario checkpoint at index $index")
}
