package me.moiclathy.dism.attention.runtime

import me.moiclathy.dism.attention.adaptation.AttentionAdaptation
import me.moiclathy.dism.attention.lifecycle.AttentionKey
import me.moiclathy.dism.attention.lifecycle.AttentionRecord
import me.moiclathy.dism.attention.memory.AttentionMemory
import kotlinx.serialization.Serializable

/** In-memory projection used by AttentionRuntime between persistence/recovery boundaries. */
@Serializable
data class AttentionRuntimeState(
    val records: Map<AttentionKey, AttentionRecord> = emptyMap(),
    val adaptations: Map<String, AttentionAdaptation> = emptyMap(),
    val memories: Map<String, AttentionMemory> = emptyMap(),
)
