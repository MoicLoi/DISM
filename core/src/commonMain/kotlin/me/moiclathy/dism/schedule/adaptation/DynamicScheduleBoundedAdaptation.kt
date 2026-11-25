package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * Boundary 后真正允许进入 Decision Layer 的结果。
 */
data class DynamicScheduleBoundedAdaptation(
    val itemId: DynamicScheduleItemId,
    val adaptationFactor: Double,
    val confidence: Double,
    val enabled: Boolean,
    val boundary:
        DynamicScheduleAdaptationBoundary,
) {
    init {
        require(adaptationFactor >= 0.0)
        require(confidence in 0.0..1.0)
    }

    companion object {

        fun from(
            boundary:
                DynamicScheduleAdaptationBoundary,
        ): DynamicScheduleBoundedAdaptation =
            DynamicScheduleBoundedAdaptation(
                itemId =
                    boundary.itemId,

                adaptationFactor =
                    1.0 +
                        boundary.allowedDelta,

                confidence =
                    boundary.confidence,

                enabled =
                    boundary.enabled,

                boundary =
                    boundary,
            )
    }
}
