package me.moiclathy.dism.attention.memory

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** Historical adaptive signal for one attention behavior key. */
@Serializable
data class AttentionMemory(
    val bias: Double = 0.0,
    val lastUpdatedAt: Instant? = null,
)
