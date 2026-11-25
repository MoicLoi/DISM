package me.moiclathy.dism.nl.e2e

data class NaturalLanguageStateE2ETrace(
    val inputText: String,
    val compiled: Boolean,
    val applied: Boolean,
    val status: String,
    val stageNames: List<String> = emptyList(),
)
