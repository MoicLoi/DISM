package me.moiclathy.dism.nl

/**
 * Intermediate representation produced by an NL compiler.
 * It is deliberately entity-free: resolution happens in a later stage.
 */
data class SemanticParseResult(
    val input: NaturalLanguageInput,
    val intents: List<SemanticIntent>,
    val unresolvedText: String = "",
    val warnings: List<String> = emptyList(),
)
