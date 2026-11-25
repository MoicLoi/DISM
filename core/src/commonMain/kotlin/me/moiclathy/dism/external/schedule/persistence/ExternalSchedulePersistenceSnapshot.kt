package me.moiclathy.dism.external.schedule.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 某一次 External Schedule Import 的持久化快照。
 */
data class ExternalSchedulePersistenceSnapshot(
    val scheduleId: ExternalScheduleId,

    val importedAt: Instant,

    val records: List<ExternalSchedulePersistenceRecord>,

    /**
     * 外部数据整体指纹。
     */
    val fingerprint: String? = null,
) {

    val size: Int
        get() = records.size

    val activeRecords: List<ExternalSchedulePersistenceRecord>
        get() = records.filter { it.active }
}
