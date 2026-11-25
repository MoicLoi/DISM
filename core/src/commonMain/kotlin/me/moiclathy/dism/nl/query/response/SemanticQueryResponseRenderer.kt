package me.moiclathy.dism.nl.query.response

import me.moiclathy.dism.nl.query.result.SemanticStateQueryAnswer

interface SemanticQueryResponseRenderer {
    fun render(
        answer: SemanticStateQueryAnswer,
        tone: SemanticQueryResponseTone = SemanticQueryResponseTone.CONCISE,
    ): SemanticQueryResponse
}
