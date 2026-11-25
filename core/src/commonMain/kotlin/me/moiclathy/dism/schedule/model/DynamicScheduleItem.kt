package me.moiclathy.dism.schedule.model

import me.moiclathy.dism.api.DismPublicApi
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The minimal schedulable unit understood by 12.x.
 *
 * This model intentionally does not contain Context, Attention, Entity, or Event
 * implementation objects. Those are bound by later 12.x stages through adapters.
 */
@Serializable
@DismPublicApi
data class DynamicScheduleItem(
    val id: DynamicScheduleItemId,
    val scheduleId: DynamicScheduleId,
    val title: String,
    val type: DynamicScheduleItemType,
    val source: DynamicScheduleSource,
    val temporal: DynamicScheduleTemporal = DynamicScheduleTemporal.Unscheduled,
    val state: DynamicScheduleState = DynamicScheduleState.UNSCHEDULED,
    val priority: Double = 0.5,
    val confidence: Double = 1.0,
    val externalKey: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(priority in 0.0..1.0) { "priority must be in [0, 1]" }
        require(confidence in 0.0..1.0) { "confidence must be in [0, 1]" }
        require(externalKey == null || externalKey.isNotBlank()) {
            "externalKey must not be blank when present"
        }
        if (createdAt != null && updatedAt != null) {
            require(updatedAt >= createdAt) { "updatedAt must be >= createdAt" }
        }
    }

    val isScheduled: Boolean
        get() = temporal !is DynamicScheduleTemporal.Unscheduled

    val isActive: Boolean
        get() = state == DynamicScheduleState.ACTIVE

    val isTerminal: Boolean
        get() = state == DynamicScheduleState.COMPLETED ||
            state == DynamicScheduleState.CANCELLED ||
            state == DynamicScheduleState.EXPIRED
}
