package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import kotlinx.serialization.Serializable

/**
 * A context dependency attached to a schedule item.
 *
 * contextKey is intentionally opaque: 12.4 does not duplicate the
 * concrete 9.x Context implementation.
 *
 * A later adapter can map the opaque key to the real ContextId,
 * ContextState, location state, activity state, or another
 * runtime context representation.
 */
@Serializable
data class DynamicScheduleContextBinding(
    val id: DynamicScheduleContextBindingId,
    val itemId: DynamicScheduleItemId,
    val contextKey: String,
    val mode: DynamicScheduleContextBindingMode =
        DynamicScheduleContextBindingMode.PREFERRED,
    val source: DynamicScheduleContextBindingSource =
        DynamicScheduleContextBindingSource.DERIVED,
    val weight: Double = 0.5,
    val confidence: Double = 1.0,
    val active: Boolean = true,
) {
    init {
        require(contextKey.isNotBlank()) {
            "contextKey must not be blank"
        }

        require(weight in 0.0..1.0) {
            "weight must be in [0, 1]"
        }

        require(confidence in 0.0..1.0) {
            "confidence must be in [0, 1]"
        }
    }

    /**
     * Weight after confidence adjustment.
     *
     * This deliberately remains deterministic.
     */
    val effectiveWeight: Double
        get() = weight * confidence
}
