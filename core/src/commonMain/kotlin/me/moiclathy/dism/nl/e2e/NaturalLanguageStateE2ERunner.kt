package me.moiclathy.dism.nl.e2e

/**
 * Framework-neutral orchestration seam for the complete NL -> state path.
 * Concrete project wiring supplies the compiler and state application callbacks.
 */
class NaturalLanguageStateE2ERunner<C, A>(
    private val compile: suspend (String) -> C?,
    private val apply: suspend (C) -> A,
    private val statusOf: (A) -> String,
) {
    suspend fun run(inputText: String): NaturalLanguageStateE2ETrace {
        val compiled = compile(inputText)
            ?: return NaturalLanguageStateE2ETrace(
                inputText = inputText,
                compiled = false,
                applied = false,
                status = "COMPILATION_REJECTED",
            )

        val appliedResult = apply(compiled)
        val status = statusOf(appliedResult)
        return NaturalLanguageStateE2ETrace(
            inputText = inputText,
            compiled = true,
            applied = status == "APPLIED",
            status = status,
            stageNames = listOf(
                "INTENT",
                "ENTITY_RESOLUTION",
                "RELATION_RESOLUTION",
                "RELATION_COMPILATION",
                "TEMPORAL_RESOLUTION",
                "OBSERVATION_COMPILATION",
                "STATE_APPLICATION",
            ),
        )
    }
}
