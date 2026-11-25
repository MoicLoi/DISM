package me.moiclathy.dism.schedule.trace

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import kotlin.time.Instant

data class DynamicScheduleDecisionTraceQuery(
    val itemId: DynamicScheduleItemId? = null,

    val from: Instant? = null,

    val until: Instant? = null,

    val stage:
        DynamicScheduleDecisionTraceStage? = null,

    val decisiveOnly: Boolean = false,

    val limit: Int = 50,
) {
    init {
        require(
            from == null ||
                until == null ||
                until >= from,
        )

        require(limit > 0)
    }
}
