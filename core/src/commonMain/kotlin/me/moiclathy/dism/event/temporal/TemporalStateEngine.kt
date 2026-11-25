package me.moiclathy.dism.event.temporal

import kotlin.time.Instant
import me.moiclathy.dism.event.model.EventTemporal

/**
 * Temporal State Engine。
 *
 * 根据：
 *
 * EventTemporal + evaluatedAt
 *
 * 推导 TemporalState。
 */
interface TemporalStateEngine {

    fun evaluate(
        temporal: EventTemporal,
        evaluatedAt: Instant,
    ): TemporalStateEvaluation
}
