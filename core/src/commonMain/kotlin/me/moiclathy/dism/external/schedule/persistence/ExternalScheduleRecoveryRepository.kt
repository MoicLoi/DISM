package me.moiclathy.dism.external.schedule.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * Recovery 所需要的最小数据访问边界。
 *
 * 11.11 不直接依赖 SQLDelight。
 */
interface ExternalScheduleRecoveryRepository {

    fun findBySchedule(
        scheduleId: ExternalScheduleId,
    ): List<ExternalSchedulePersistenceRecord>
}
