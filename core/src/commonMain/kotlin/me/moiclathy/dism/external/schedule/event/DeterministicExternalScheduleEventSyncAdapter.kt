package me.moiclathy.dism.external.schedule.event

import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncChange

/**
 * 确定性的 External Schedule Event Sync Adapter。
 *
 * 它只负责把 Sync Change 转发给注入的 Event 操作实现。
 *
 * 因此：
 *
 * 11.15
 * Sync Runtime
 *       ↓
 * 11.16
 * Event Sync Adapter
 *       ↓
 * 10.x Event API
 */
class DeterministicExternalScheduleEventSyncAdapter(
    private val eventHandler:
        ExternalScheduleEventSyncAdapter,

    private val policy:
        ExternalScheduleEventSyncPolicy =
        ExternalScheduleEventSyncPolicy(),
) {

    fun execute(
        change:
            ExternalScheduleSyncChange,
    ): ExternalScheduleEventSyncResult {

        if (
            change.externalKey.isBlank()
        ) {

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .INVALID_EXTERNAL_KEY,

                message =
                    "externalKey must not be blank",
            )
        }

        return when (
            change.action
        ) {

            ExternalScheduleSyncAction.CREATE_EVENT ->
                executeCreate(change)

            ExternalScheduleSyncAction.UPDATE_EVENT ->
                executeUpdate(change)

            ExternalScheduleSyncAction
                .HANDLE_REMOVED_EVENT ->
                executeRemoval(change)

            ExternalScheduleSyncAction
                .RECREATE_EVENT_MAPPING ->
                executeUpdate(change)

            ExternalScheduleSyncAction.NO_OP ->
                ExternalScheduleEventSyncResult.success(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    eventId =
                        change.diff
                            .current
                            ?.eventId
                            ?: change.diff
                                .previous
                                ?.eventId,
                )
        }
    }

    private fun executeCreate(
        change:
            ExternalScheduleSyncChange,
    ): ExternalScheduleEventSyncResult {

        if (!policy.allowCreate) {

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .CREATE_FAILED,

                message =
                    "Event creation is disabled",
            )
        }

        val current =
            change.diff.current

                ?: return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    error =
                        ExternalScheduleEventSyncError
                            .MISSING_CURRENT_RECORD,

                    message =
                        "CREATE_EVENT requires current record",
                )

        try {

            val eventId =
                eventHandler.create(
                    change
                )

            if (
                eventId.isNullOrBlank()
            ) {

                return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    error =
                        ExternalScheduleEventSyncError
                            .CREATE_FAILED,

                    message =
                        "Event handler returned no event ID",
                )
            }

            return ExternalScheduleEventSyncResult.success(
                action =
                    change.action.name,

                externalKey =
                    current.externalKey,

                eventId =
                    eventId,
            )

        } catch (e: Exception) {

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .CREATE_FAILED,

                message =
                    e.message
                        ?: "Unknown create failure",
            )
        }
    }

    private fun executeUpdate(
        change:
            ExternalScheduleSyncChange,
    ): ExternalScheduleEventSyncResult {

        if (!policy.allowUpdate) {

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .UPDATE_FAILED,

                message =
                    "Event update is disabled",
            )
        }

        val previous =
            change.diff.previous

                ?: return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    error =
                        ExternalScheduleEventSyncError
                            .MISSING_PREVIOUS_RECORD,

                    message =
                        "UPDATE_EVENT requires previous record",
                )

        val current =
            change.diff.current

                ?: return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    error =
                        ExternalScheduleEventSyncError
                            .MISSING_CURRENT_RECORD,

                    message =
                        "UPDATE_EVENT requires current record",
                )

        val previousEventId =
            previous.eventId

        if (
            policy.preserveEventIdOnUpdate &&
            previousEventId.isNullOrBlank()
        ) {

            if (
                policy.recreateMissingEventMapping
            ) {

                return executeCreate(
                    change =
                        change.copy(
                            action =
                                ExternalScheduleSyncAction
                                    .CREATE_EVENT,
                        )
                )
            }

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .MISSING_EVENT_MAPPING,

                message =
                    "Changed record has no existing Event mapping",
            )
        }

        try {

            val eventId =
                eventHandler.update(
                    change
                )

            if (
                eventId.isNullOrBlank()
            ) {

                return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    error =
                        ExternalScheduleEventSyncError
                            .UPDATE_FAILED,

                    message =
                        "Event handler returned no event ID",
                )
            }

            if (
                policy.preserveEventIdOnUpdate &&
                previousEventId != null &&
                eventId != previousEventId
            ) {

                return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    eventId =
                        eventId,

                    error =
                        ExternalScheduleEventSyncError
                            .EVENT_ID_NOT_PRESERVED,

                    message =
                        "Update changed Event ID from " +
                            "$previousEventId to $eventId",
                )
            }

            return ExternalScheduleEventSyncResult.success(
                action =
                    change.action.name,

                externalKey =
                    current.externalKey,

                eventId =
                    eventId,
            )

        } catch (e: Exception) {

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .UPDATE_FAILED,

                message =
                    e.message
                        ?: "Unknown update failure",
            )
        }
    }

    private fun executeRemoval(
        change:
            ExternalScheduleSyncChange,
    ): ExternalScheduleEventSyncResult {

        if (
            !policy.handleRemovedEvents
        ) {

            return ExternalScheduleEventSyncResult.success(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                eventId =
                    change.diff
                        .previous
                        ?.eventId,
            )
        }

        val previous =
            change.diff.previous

                ?: return ExternalScheduleEventSyncResult.failure(
                    action =
                        change.action.name,

                    externalKey =
                        change.externalKey,

                    error =
                        ExternalScheduleEventSyncError
                            .MISSING_PREVIOUS_RECORD,

                    message =
                        "Removed record has no previous record",
                )

        try {

            val eventId =
                eventHandler.handleRemoved(
                    change
                )

            return ExternalScheduleEventSyncResult.success(
                action =
                    change.action.name,

                externalKey =
                    previous.externalKey,

                eventId =
                    eventId
                        ?: previous.eventId,
            )

        } catch (e: Exception) {

            return ExternalScheduleEventSyncResult.failure(
                action =
                    change.action.name,

                externalKey =
                    change.externalKey,

                error =
                    ExternalScheduleEventSyncError
                        .REMOVAL_FAILED,

                message =
                    e.message
                        ?: "Unknown removal failure",
            )
        }
    }
}
