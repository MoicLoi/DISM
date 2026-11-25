package me.moiclathy.dism.event.duration

import kotlin.time.Duration

/**
 * 确定性的 Duration Engine。
 */
class DeterministicEventDurationEngine(
    private val policy:
        EventDurationPolicy =
        EventDurationPolicy(),
) : EventDurationEngine {

    override fun resolve(
        duration: EventDuration,
    ): EventDurationResult {

        validateType(
            duration.type,
        )

        if (
            !policy.allowZeroDuration &&
            duration.value == Duration.ZERO
        ) {
            throw IllegalArgumentException(
                "Zero duration is not allowed by policy."
            )
        }

        val estimate =
            EventDurationEstimate(
                minimum =
                    duration.value,

                expected =
                    duration.value,

                maximum =
                    duration.value,
            )

        return EventDurationResult(
            duration =
                duration,

            estimate =
                estimate,
        )
    }

    override fun resolveEstimate(
        estimate: EventDurationEstimate,
        type: EventDurationType,
    ): EventDurationResult {

        validateType(type)

        return EventDurationResult(
            duration =
                EventDuration(
                    value =
                        estimate.expected,

                    type =
                        type,
                ),

            estimate =
                estimate,
        )
    }

    private fun validateType(
        type: EventDurationType,
    ) {

        when (type) {

            EventDurationType.FIXED -> {
                // Always allowed.
            }

            EventDurationType.ESTIMATED -> {
                require(policy.allowEstimated) {
                    "Estimated duration is disabled by policy."
                }
            }

            EventDurationType.DERIVED -> {
                require(policy.allowDerived) {
                    "Derived duration is disabled by policy."
                }
            }
        }
    }
}
