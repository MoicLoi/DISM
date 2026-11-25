package me.moiclathy.dism.external.schedule.diff

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * External Schedule Diff 接口。
 */
interface ExternalScheduleDiffer {

    fun diff(
        scheduleId: ExternalScheduleId,
        previous: List<ExternalSchedulePersistenceRecord>,
        current: List<ExternalSchedulePersistenceRecord>,
        policy: ExternalScheduleDiffPolicy =
            ExternalScheduleDiffPolicy(),
    ): ExternalScheduleDiffResult
}
