package me.moiclathy.dism.external.schedule.sync.idempotency

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Stable identity of one logical sync action.
 *
 * A retry of the same schedule/run/external-key/action must resolve to the
 * same identity so that Event creation cannot be duplicated accidentally.
 */
data class ExternalScheduleSyncActionIdentity(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val externalKey: String,
    val action: ExternalScheduleSyncAction,
) {
    init {
        require(externalKey.isNotBlank()) { "externalKey must not be blank" }
    }

    val value: String
        get() = buildString {
            append(scheduleId.value)
            append(':')
            append(runId.value)
            append(':')
            append(externalKey)
            append(':')
            append(action.name)
        }
}
