package me.moiclathy.dism.schedule.model

import me.moiclathy.dism.api.DismPublicApi
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@DismPublicApi
sealed interface DynamicScheduleTemporal {
    @Serializable
    data object Unscheduled : DynamicScheduleTemporal

    @Serializable
    data class At(
        override val startAt: Instant,
    ) : DynamicScheduleTemporal

    @Serializable
    data class Interval(
        override val startAt: Instant,
        override val endAt: Instant,
    ) : DynamicScheduleTemporal {
        init {
            require(endAt >= startAt) { "endAt must be >= startAt" }
        }
    }

    @Serializable
    data class Deadline(
        val deadline: Instant,
    ) : DynamicScheduleTemporal

    @Serializable
    data class Window(
        val earliestStart: Instant,
        val latestStart: Instant,
    ) : DynamicScheduleTemporal {
        init {
            require(latestStart >= earliestStart) {
                "latestStart must be >= earliestStart"
            }
        }
    }

    val startAt: Instant?
        get() = when (this) {
            Unscheduled -> null
            is At -> startAt
            is Interval -> startAt
            is Deadline -> null
            is Window -> earliestStart
        }

    val endAt: Instant?
        get() = when (this) {
            Unscheduled -> null
            is At -> null
            is Interval -> endAt
            is Deadline -> deadline
            is Window -> null
        }
}
