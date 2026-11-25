package me.moiclathy.dism.e2e.adapter

import me.moiclathy.dism.attention.adaptation.AttentionFeedback
import me.moiclathy.dism.attention.runtime.AttentionRuntime
import me.moiclathy.dism.attention.runtime.AttentionRuntimeState
import kotlin.time.Instant

/** Maps UI/domain feedback into the real AttentionRuntime feedback contract. */
class AttentionRuntimeFeedbackAdapter(
    private val runtime: AttentionRuntime,
) {
    fun apply(
        state: AttentionRuntimeState,
        attentionKey: String,
        action: me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction,
        observedAt: Instant,
        eventLog: me.moiclathy.dism.attention.persistence.AttentionEventLog? = null,
    ): AttentionRuntimeState {
        require(attentionKey.isNotBlank())
        return runtime.applyFeedback(
            state = state,
            feedback = AttentionFeedback(
                attentionKey = attentionKey,
                action = action,
                observedAt = observedAt,
            ),
            eventLog = eventLog,
        )
    }
}
