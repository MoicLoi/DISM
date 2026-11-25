package me.moiclathy.dism.nl

/** Small deterministic normalization layer; it never invents entities or facts. */
object DeterministicSemanticNormalizer {
    fun normalize(input: NaturalLanguageInput): NaturalLanguageInput =
        input.copy(text = input.text.trim().replace(Regex("\\s+"), " "))
}
