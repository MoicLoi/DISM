package me.moiclathy.dism.nl.query.response

import kotlin.test.Test
import kotlin.test.assertEquals
import me.moiclathy.dism.nl.query.SemanticStateQuery
import me.moiclathy.dism.nl.query.result.SemanticStateQueryAnswer
import me.moiclathy.dism.nl.query.result.SemanticStateQueryAnswerStatus

class DeterministicSemanticQueryResponseRendererTest {
    private val renderer = DeterministicSemanticQueryResponseRenderer()

    private fun answer(status: SemanticStateQueryAnswerStatus, values: List<String> = emptyList()) =
        SemanticStateQueryAnswer(
            query = SemanticStateQuery(),
            status = status,
            values = values,
        )

    @Test
    fun answeredUsesOnlyReturnedValues() {
        val result = renderer.render(answer(SemanticStateQueryAnswerStatus.ANSWERED, listOf("宿舍")))
        assertEquals("宿舍", result.text)
        assertEquals(SemanticQueryResponseStatus.ANSWERED, result.status)
    }

    @Test
    fun explanatoryToneDoesNotInventEvidence() {
        val result = renderer.render(
            answer(SemanticStateQueryAnswerStatus.ANSWERED, listOf("宿舍", "实验楼")),
            SemanticQueryResponseTone.EXPLANATORY,
        )
        assertEquals("当前状态：宿舍、实验楼。", result.text)
    }

    @Test
    fun ambiguousRemainsAmbiguous() {
        val result = renderer.render(answer(SemanticStateQueryAnswerStatus.AMBIGUOUS))
        assertEquals(SemanticQueryResponseStatus.AMBIGUOUS, result.status)
    }
}
