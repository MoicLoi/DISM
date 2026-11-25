package me.moiclathy.dism.external.schedule.sync.reconciliation

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord

/**
 * Sync / Event Reconciliation 边界。
 */
interface ExternalScheduleSyncReconciliationService {

    /**
     * 对当前 External Schedule 与 Sync History
     * 进行 Event 层状态检查。
     */
    fun reconcile(
        scheduleId:
            ExternalScheduleId,

        currentRecords:
            List<ExternalSchedulePersistenceRecord>,

        syncRecords:
            List<ExternalScheduleSyncPersistenceRecord>,
    ):
        ExternalScheduleSyncReconciliationResult
}
