package me.moiclathy.dism.external.schedule.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 某一个 External Schedule 恢复后的完整快照。
 */
data class ExternalScheduleRecoverySnapshot(
    val scheduleId: ExternalScheduleId,
    val records: List<ExternalScheduleRecoveryRecord>,
) {

    val size: Int
        get() = records.size

    val restoredRecords: List<ExternalScheduleRecoveryRecord>
        get() = records.filter {
            it.status == ExternalScheduleRecoveryStatus.RESTORED
        }

    val inactiveRecords: List<ExternalScheduleRecoveryRecord>
        get() = records.filter {
            it.status == ExternalScheduleRecoveryStatus.INACTIVE
        }

    val missingEventMappings:
        List<ExternalScheduleRecoveryRecord>
        get() = records.filter {
            it.status ==
                ExternalScheduleRecoveryStatus.MISSING_EVENT_MAPPING
        }

    val invalidRecords:
        List<ExternalScheduleRecoveryRecord>
        get() = records.filter {
            it.status ==
                ExternalScheduleRecoveryStatus.INVALID
        }

    val recoverableRecords:
        List<ExternalScheduleRecoveryRecord>
        get() = records.filter {
            it.recoverable
        }
}
