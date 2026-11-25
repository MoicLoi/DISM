package me.moiclathy.dism.external.schedule.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * Recovery Repository 的内存实现。
 */
class InMemoryExternalScheduleRecoveryRepository(
    private val records:
        MutableList<ExternalSchedulePersistenceRecord> =
        mutableListOf(),
) : ExternalScheduleRecoveryRepository {

    override fun findBySchedule(
        scheduleId: ExternalScheduleId,
    ): List<ExternalSchedulePersistenceRecord> {

        return records
            .filter {
                it.externalScheduleId == scheduleId
            }
            .toList()
    }

    fun add(
        record: ExternalSchedulePersistenceRecord,
    ) {
        records.removeAll {
            it.externalScheduleId ==
                record.externalScheduleId &&
                it.externalKey ==
                record.externalKey
        }

        records += record
    }

    fun addAll(
        records: Collection<ExternalSchedulePersistenceRecord>,
    ) {
        records.forEach {
            add(it)
        }
    }

    fun clear() {
        records.clear()
    }
}
