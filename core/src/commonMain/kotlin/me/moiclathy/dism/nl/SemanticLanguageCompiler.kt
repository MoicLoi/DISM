package me.moiclathy.dism.nl

/** Boundary for LLM, rules, or other language frontends. */
fun interface SemanticLanguageCompiler {
    suspend fun compile(input: NaturalLanguageInput): SemanticParseResult
}
