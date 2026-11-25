package me.moiclathy.dism.attention.persistence.bootstrap

import kotlin.time.Instant
import me.moiclathy.dism.attention.recovery.AttentionRecoveryState

/** Immutable projection produced before the first AttentionRuntime evaluation. */
data class AttentionStartupState(
    val recovered: Map<String, AttentionRecoveryState>,
    val restoredAt: Instant,
    val eventCount: Int,
) {
    val attentionCount: Int get() = recovered.size
}
