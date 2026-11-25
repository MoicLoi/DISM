package me.moiclathy.dism.external.schedule.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * External Schedule Recovery Service。
 *
 * 负责把 Persistence 层的数据重新解释成
 * 可供 Runtime 使用的 Recovery Snapshot。
 *
 * 不负责：
 *
 * - 创建 Event
 * - 修改 Event
 * - 重新解析 ICS
 * - Entity Resolution
 * - Attention
 */
class ExternalScheduleRecoveryService(
    private val repository:
        ExternalScheduleRecoveryRepository,
) {

    fun recover(
        scheduleId: ExternalScheduleId,
    ): ExternalScheduleRecoveryResult {

        val records =
            repository.findBySchedule(
                scheduleId
            )

        if (records.isEmpty()) {

            return ExternalScheduleRecoveryResult.success(
                snapshot =
                    ExternalScheduleRecoverySnapshot(
                        scheduleId = scheduleId,
                        records = emptyList(),
                    ),
                warnings =
                    listOf(
                        "No persisted records found"
                    ),
            )
        }

        val recovered =
            records.map {
                recoverRecord(it)
            }

        val warnings =
            recovered
                .mapNotNull {
                    if (it.reason != null) {
                        "${it.record.externalKey}: ${it.reason}"
                    } else {
                        null
                    }
                }

        return ExternalScheduleRecoveryResult.success(
            snapshot =
                ExternalScheduleRecoverySnapshot(
                    scheduleId =
                        scheduleId,

                    records =
                        recovered,
                ),
            warnings =
                warnings,
        )
    }

    private fun recoverRecord(
        record:
            ExternalSchedulePersistenceRecord,
    ): ExternalScheduleRecoveryRecord {

        if (!record.active) {

            return ExternalScheduleRecoveryRecord(
                record = record,

                status =
                    ExternalScheduleRecoveryStatus.INACTIVE,

                recoverable = false,

                reason =
                    "External record is inactive",
            )
        }

        if (record.eventId == null) {

            return ExternalScheduleRecoveryRecord(
                record = record,

                status =
                    ExternalScheduleRecoveryStatus
                        .MISSING_EVENT_MAPPING,

                recoverable = true,

                reason =
                    "External record has no mapped Event",
            )
        }

        if (record.externalKey.isBlank()) {

            return ExternalScheduleRecoveryRecord(
                record = record,

                status =
                    ExternalScheduleRecoveryStatus.INVALID,

                recoverable = false,

                reason =
                    "External key is blank",
            )
        }

        if (record.title.isBlank()) {

            return ExternalScheduleRecoveryRecord(
                record = record,

                status =
                    ExternalScheduleRecoveryStatus.INVALID,

                recoverable = false,

                reason =
                    "Title is blank",
            )
        }

        if (
            record.startAt != null &&
            record.endAt != null &&
            record.endAt < record.startAt
        ) {

            return ExternalScheduleRecoveryRecord(
                record = record,

                status =
                    ExternalScheduleRecoveryStatus.INVALID,

                recoverable = false,

                reason =
                    "Invalid temporal range",
            )
        }

        return ExternalScheduleRecoveryRecord(
            record = record,

            status =
                ExternalScheduleRecoveryStatus.RESTORED,

            recoverable = true,
        )
    }
}
