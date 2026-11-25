package me.moiclathy.dism.event.temporal

import kotlin.time.Instant

/**
 * Temporal State 计算结果。
 */
data class TemporalStateEvaluation(
    val state: TemporalState,
    val evaluatedAt: Instant,
)
