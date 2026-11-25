package me.moiclathy.dism.external.schedule.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 内存 Persistence。
 *
 * 用于：
 *
 * - Unit Test
 * - E2E Fixture
 * - Android/Desktop 尚未接入 DB 时的运行验证
 */
class InMemoryExternalSchedulePersistenceRepository :
    ExternalSchedulePersistenceRepository {

    private val records =
        LinkedHashMap<String, ExternalSchedulePersistenceRecord>()

    override fun upsert(
        record: ExternalSchedulePersistenceRecord,
    ): ExternalSchedulePersistenceResult {

        records[key(record.externalScheduleId, record.externalKey)] =
            record

        return ExternalSchedulePersistenceResult.success(
            affectedRecords = 1,
        )
    }

    override fun upsertAll(
        records: List<ExternalSchedulePersistenceRecord>,
    ): ExternalSchedulePersistenceResult {

        records.forEach { record ->
            upsert(record)
        }

        return ExternalSchedulePersistenceResult.success(
            affectedRecords = records.size,
        )
    }

    override fun find(
        scheduleId: ExternalScheduleId,
        externalKey: String,
    ): ExternalSchedulePersistenceRecord? {

        return records[
            key(
                scheduleId,
                externalKey,
            )
        ]
    }

    override fun findBySchedule(
        scheduleId: ExternalScheduleId,
    ): List<ExternalSchedulePersistenceRecord> {

        return records.values
            .filter {
                it.externalScheduleId == scheduleId
            }
            .toList()
    }

    override fun delete(
        scheduleId: ExternalScheduleId,
        externalKey: String,
    ): ExternalSchedulePersistenceResult {

        val removed =
            records.remove(
                key(
                    scheduleId,
                    externalKey,
                )
            )

        return ExternalSchedulePersistenceResult.success(
            affectedRecords =
                if (removed != null) 1 else 0,
        )
    }

    override fun deactivateMissing(
        scheduleId: ExternalScheduleId,
        activeExternalKeys: Set<String>,
        seenAt: Instant,
    ): ExternalSchedulePersistenceResult {

        var affected = 0

        val scheduleRecords =
            records.values
                .filter {
                    it.externalScheduleId == scheduleId
                }

        for (record in scheduleRecords) {

            if (
                record.active &&
                record.externalKey !in activeExternalKeys
            ) {

                val updated =
                    record.copy(
                        active = false,
                        lastSeenAt = seenAt,
                    )

                records[
                    key(
                        record.externalScheduleId,
                        record.externalKey,
                    )
                ] = updated

                affected++
            }
        }

        return ExternalSchedulePersistenceResult.success(
            affectedRecords = affected,
        )
    }

    private fun key(
        scheduleId: ExternalScheduleId,
        externalKey: String,
    ): String =
        "${scheduleId.value}:$externalKey"
}
