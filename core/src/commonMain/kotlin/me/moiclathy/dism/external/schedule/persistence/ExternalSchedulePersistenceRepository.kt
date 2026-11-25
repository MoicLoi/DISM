package me.moiclathy.dism.external.schedule.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * External Schedule Persistence Repository。
 *
 * 数据库实现可以是：
 *
 * - SQLDelight
 * - SQLite
 * - In-memory
 * - 其他平台实现
 */
interface ExternalSchedulePersistenceRepository {

    fun upsert(
        record: ExternalSchedulePersistenceRecord,
    ): ExternalSchedulePersistenceResult

    fun upsertAll(
        records: List<ExternalSchedulePersistenceRecord>,
    ): ExternalSchedulePersistenceResult

    fun find(
        scheduleId: ExternalScheduleId,
        externalKey: String,
    ): ExternalSchedulePersistenceRecord?

    fun findBySchedule(
        scheduleId: ExternalScheduleId,
    ): List<ExternalSchedulePersistenceRecord>

    fun delete(
        scheduleId: ExternalScheduleId,
        externalKey: String,
    ): ExternalSchedulePersistenceResult

    fun deactivateMissing(
        scheduleId: ExternalScheduleId,
        activeExternalKeys: Set<String>,
        seenAt: kotlin.time.Instant,
    ): ExternalSchedulePersistenceResult
}
