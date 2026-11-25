package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.attention.runtime.AttentionRuntimeCandidate
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * A schedule item projected into the Attention subsystem.
 *
 * The schedule layer remains the source of schedule semantics.
 * AttentionRuntime remains responsible for final attention selection.
 */
data class DynamicScheduleAttentionCandidate(
    val itemId: DynamicScheduleItemId,
    val candidate: AttentionRuntimeCandidate,
    val schedulePriority: Double,
    val scheduleConfidence: Double,
) {
    init {
        require(schedulePriority in 0.0..1.0) {
            "schedulePriority must be in [0, 1]"
        }

        require(scheduleConfidence in 0.0..1.0) {
            "scheduleConfidence must be in [0, 1]"
        }
    }
}
