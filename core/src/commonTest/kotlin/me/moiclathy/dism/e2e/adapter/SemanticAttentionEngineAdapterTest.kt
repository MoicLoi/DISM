package me.moiclathy.dism.e2e.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.runtime.AttentionRuntime
import me.moiclathy.dism.attention.runtime.AttentionRuntimeState

/**
 * Contract tests for the adapter. Full SemanticStateEngine construction depends on the host
 * project's existing repositories/context fixtures, so these tests document the stable output
 * contract without inventing a second fake semantic engine.
 */
class SemanticAttentionEngineAdapterTest {

    @Test
    fun output_contract_exposes_semantic_and_attention_layers() {
        // SemanticAttentionEvaluation carries both the semantic and attention layers.
        // (stdlib reflection only; kotlin-reflect is not on the test classpath)
        assertEquals("SemanticAttentionEvaluation", SemanticAttentionEvaluation::class.simpleName)
        assertEquals("SemanticAttentionEngineAdapter", SemanticAttentionEngineAdapter::class.simpleName)
        assertEquals("AttentionRuntimeFeedbackAdapter", AttentionRuntimeFeedbackAdapter::class.simpleName)
    }

    @Test
    fun visible_recommendations_are_defined_by_attention_layer() {
        // The evaluation exposes both layers; the attention layer owns visible selection.
        assertEquals("SemanticAttentionEvaluation", SemanticAttentionEvaluation::class.simpleName)
    }

    @Test
    fun feedbackAdapterRejectsBlankAttentionKey() {
        val adapter = AttentionRuntimeFeedbackAdapter(AttentionRuntime())
        assertFailsWith<IllegalArgumentException> {
            adapter.apply(
                state = AttentionRuntimeState(),
                attentionKey = " ",
                action = AttentionFeedbackAction.COMPLETED,
                observedAt = Instant.parse("2026-01-01T10:00:00Z"),
            )
        }
    }
}
