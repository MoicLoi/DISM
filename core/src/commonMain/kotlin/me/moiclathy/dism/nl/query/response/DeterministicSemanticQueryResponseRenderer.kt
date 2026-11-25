package me.moiclathy.dism.nl.query.response

import me.moiclathy.dism.nl.query.result.SemanticStateQueryAnswer
import me.moiclathy.dism.nl.query.result.SemanticStateQueryAnswerStatus

/**
 * Conservative renderer. It only uses information already present in the structured answer;
 * it never invents state, entities, confidence, or explanations.
 */
class DeterministicSemanticQueryResponseRenderer : SemanticQueryResponseRenderer {
    override fun render(
        answer: SemanticStateQueryAnswer,
        tone: SemanticQueryResponseTone,
    ): SemanticQueryResponse {
        val text = when (answer.status) {
            SemanticStateQueryAnswerStatus.ANSWERED -> renderAnswered(answer, tone)
            SemanticStateQueryAnswerStatus.NOT_FOUND -> "没有找到对应状态。"
            SemanticStateQueryAnswerStatus.AMBIGUOUS -> "存在多个可能的对象，暂时无法确定。"
            SemanticStateQueryAnswerStatus.UNAVAILABLE -> "当前无法查询这个状态。"
        }
        return SemanticQueryResponse(
            status = SemanticQueryResponseStatus.valueOf(answer.status.name),
            text = text,
            answer = answer,
            confidence = answer.confidence,
        )
    }

    private fun renderAnswered(
        answer: SemanticStateQueryAnswer,
        tone: SemanticQueryResponseTone,
    ): String {
        val values = answer.values
        if (values.isEmpty()) return "已找到状态，但没有可展示的值。"
        val joined = values.joinToString("、")
        return when (tone) {
            SemanticQueryResponseTone.CONCISE -> joined
            SemanticQueryResponseTone.EXPLANATORY -> "当前状态：$joined。"
        }
    }
}
