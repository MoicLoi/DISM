package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord

/**
 * 一条 Recovery 判断结果。
 */
data class ExternalScheduleSyncRecoveryRecord(

    val scheduleId:
        ExternalScheduleId,

    val externalKey:
        String,

    val action:
        ExternalScheduleSyncRecoveryAction,

    val reason:
        ExternalScheduleSyncRecoveryReason,

    val syncRecord:
        ExternalScheduleSyncPersistenceRecord? = null,

    val currentRecord:
        ExternalSchedulePersistenceRecord? = null,

    val message:
        String? = null,
) {

    init {

        require(
            externalKey.isNotBlank()
        ) {
            "externalKey must not be blank"
        }

        require(
            syncRecord == null ||
                syncRecord.externalKey == externalKey
        ) {
            "syncRecord externalKey must match"
        }

        require(
            currentRecord == null ||
                currentRecord.externalKey == externalKey
        ) {
            "currentRecord externalKey must match"
        }
    }

    val recoverable: Boolean
        get() =
            action !=
                ExternalScheduleSyncRecoveryAction.SKIP
}
