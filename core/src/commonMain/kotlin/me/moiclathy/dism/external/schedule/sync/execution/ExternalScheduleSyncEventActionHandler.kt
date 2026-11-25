package me.moiclathy.dism.external.schedule.sync.execution

import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * Execution port used by 11.30.
 *
 * The concrete implementation belongs to the Event layer (11.16).
 * This keeps the recovery executor unaware of Event construction and
 * Event implementation details.
 */
interface ExternalScheduleSyncEventActionHandler {

    fun create(
        current: ExternalSchedulePersistenceRecord,
    ): String?

    fun update(
        previous: ExternalSchedulePersistenceRecord,
        current: ExternalSchedulePersistenceRecord,
    ): String?

    fun handleRemoved(
        previous: ExternalSchedulePersistenceRecord,
    ): String?

    fun recreateEventMapping(
        previous: ExternalSchedulePersistenceRecord?,
        current: ExternalSchedulePersistenceRecord?,
    ): String?
}
