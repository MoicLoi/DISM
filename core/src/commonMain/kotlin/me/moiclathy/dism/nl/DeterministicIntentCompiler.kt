package me.moiclathy.dism.nl

/** Stable functional entry point for deterministic intent extraction. */
object DeterministicIntentCompiler {
    private val delegate = IntentCompiler()

    fun compile(input: NaturalLanguageInput): SemanticParseResult = delegate.compile(input)
}
