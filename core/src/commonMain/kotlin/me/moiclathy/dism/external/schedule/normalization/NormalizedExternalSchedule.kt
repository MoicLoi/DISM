package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

/**
 * 规范化后的完整课程表。
 */
data class NormalizedExternalSchedule(
    val id: ExternalScheduleId,
    val source: ExternalScheduleOrigin,
    val name: String,
    val normalizedAt: Instant,
    val entries: List<NormalizedExternalScheduleEntry>,
) {

    init {
        require(name.isNotBlank()) {
            "Normalized schedule name must not be blank."
        }
    }

    val size: Int
        get() =
            entries.size

    fun entry(
        id: ExternalScheduleId,
    ): NormalizedExternalScheduleEntry? =
        entries.firstOrNull {
            it.id == id
        }
}
