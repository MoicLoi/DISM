package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * External Schedule Sync Recovery Service。
 *
 * 只负责：
 *
 * Sync History
 *      +
 * Current External State
 *      ↓
 * Recovery Plan
 */
interface ExternalScheduleSyncRecoveryService {

    fun recover(
        scheduleId:
            ExternalScheduleId,

        currentRecords:
            List<ExternalSchedulePersistenceRecord>,

        policy:
            ExternalScheduleSyncRecoveryPolicy =
            ExternalScheduleSyncRecoveryPolicy(),
    ): ExternalScheduleSyncRecoveryResult
}
