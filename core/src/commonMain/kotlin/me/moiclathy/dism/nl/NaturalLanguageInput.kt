package me.moiclathy.dism.nl

import kotlin.time.Instant

/** A user utterance before semantic compilation. */
data class NaturalLanguageInput(
    val text: String,
    val source: String = "user",
    val observedAt: Instant,
    val conversationId: String? = null,
)
