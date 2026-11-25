package me.moiclathy.dism.external.schedule.sync.reconciliation

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord

/**
 * 一条 Reconciliation 结果。
 */
data class ExternalScheduleSyncReconciliationRecord(

    val scheduleId:
        ExternalScheduleId,

    val externalKey:
        String,

    val status:
        ExternalScheduleSyncReconciliationStatus,

    val action:
        ExternalScheduleSyncReconciliationAction,

    val reason:
        ExternalScheduleSyncReconciliationReason,

    val syncRecord:
        ExternalScheduleSyncPersistenceRecord? = null,

    val currentRecord:
        ExternalSchedulePersistenceRecord? = null,

    val eventExists:
        Boolean = false,

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

    val requiresAction: Boolean
        get() =
            action !=
                ExternalScheduleSyncReconciliationAction
                    .NO_OP
}
