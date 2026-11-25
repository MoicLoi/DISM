package me.moiclathy.dism.external.schedule.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * External Schedule Persistence Service。
 *
 * 负责一次 Import 的持久化生命周期：
 *
 * 1. 写入本次出现的 records
 * 2. 标记本次没有出现的 records 为 inactive
 */
class ExternalSchedulePersistenceService(
    private val repository:
        ExternalSchedulePersistenceRepository,
) {

    fun persistSnapshot(
        snapshot: ExternalSchedulePersistenceSnapshot,
    ): ExternalSchedulePersistenceResult {

        val upsertResult =
            repository.upsertAll(
                snapshot.records
            )

        if (!upsertResult.successful) {
            return upsertResult
        }

        val activeKeys =
            snapshot.records
                .map {
                    it.externalKey
                }
                .toSet()

        val deactivateResult =
            repository.deactivateMissing(
                scheduleId =
                    snapshot.scheduleId,

                activeExternalKeys =
                    activeKeys,

                seenAt =
                    snapshot.importedAt,
            )

        if (!deactivateResult.successful) {
            return deactivateResult
        }

        return ExternalSchedulePersistenceResult.success(
            affectedRecords =
                upsertResult.affectedRecords +
                    deactivateResult.affectedRecords,

            warnings =
                upsertResult.warnings +
                    deactivateResult.warnings,
        )
    }

    fun find(
        scheduleId: ExternalScheduleId,
        externalKey: String,
    ): ExternalSchedulePersistenceRecord? =
        repository.find(
            scheduleId,
            externalKey,
        )

    fun findBySchedule(
        scheduleId: ExternalScheduleId,
    ): List<ExternalSchedulePersistenceRecord> =
        repository.findBySchedule(
            scheduleId
        )
}
