package me.moiclathy.dism.attention.runtime

import me.moiclathy.dism.attention.AttentionSelectionResult
import me.moiclathy.dism.attention.context.AttentionContextDecision
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation
import me.moiclathy.dism.attention.lifecycle.AttentionDecision
import me.moiclathy.dism.attention.persistence.AttentionEvent

/** Complete output of one attention evaluation cycle. */
data class AttentionRuntimeResult(
    val selection: AttentionSelectionResult,
    val context: AttentionContextDecision,
    val lifecycleDecisions: List<AttentionDecision>,
    val explanations: Map<String, AttentionDecisionExplanation>,
    val events: List<AttentionEvent>,
    val nextState: AttentionRuntimeState,
)
