package me.moiclathy.dism.schedule.snapshot

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleDecisionSnapshotQuery(
    val itemId: DynamicScheduleItemId? = null,

    val from: Instant? = null,

    val until: Instant? = null,

    val scheduleRevision: Long? = null,

    val engineVersion: String? = null,

    val limit: Int = 50,
) {
    init {
        require(
            from == null ||
                until == null ||
                until >= from,
        )

        require(
            scheduleRevision == null ||
                scheduleRevision >= 0,
        )

        require(
            engineVersion == null ||
                engineVersion.isNotBlank(),
        )

        require(limit > 0)
    }
}
