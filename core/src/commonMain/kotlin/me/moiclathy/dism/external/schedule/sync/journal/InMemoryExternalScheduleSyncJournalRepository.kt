package me.moiclathy.dism.external.schedule.sync.journal

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * 内存 Journal Repository。
 */
class InMemoryExternalScheduleSyncJournalRepository :
    ExternalScheduleSyncJournalRepository {

    private val events =
        linkedMapOf<
            String,
            ExternalScheduleSyncJournalEvent
        >()

    override fun append(
        event:
            ExternalScheduleSyncJournalEvent,
    ):
        ExternalScheduleSyncJournalEvent {

        if (
            events.containsKey(
                event.journalId.value
            )
        ) {
            return events[
                event.journalId.value
            ]!!
        }

        events[
            event.journalId.value
        ] = event

        return event
    }

    override fun appendAll(
        events:
            List<ExternalScheduleSyncJournalEvent>,
    ):
        List<ExternalScheduleSyncJournalEvent> {

        return events.map {
            append(it)
        }
    }

    override fun findByRun(
        runId:
            ExternalScheduleSyncRunId,
    ):
        List<ExternalScheduleSyncJournalEvent> {

        return events
            .values
            .filter {
                it.runId == runId
            }
            .sortedWith(
                compareBy<
                    ExternalScheduleSyncJournalEvent
                > {
                    it.occurredAt
                }.thenBy {
                    it.journalId.value
                }
            )
    }

    override fun findBySchedule(
        scheduleId:
            ExternalScheduleId,
    ):
        List<ExternalScheduleSyncJournalEvent> {

        return events
            .values
            .filter {
                it.scheduleId == scheduleId
            }
            .sortedWith(
                compareBy<
                    ExternalScheduleSyncJournalEvent
                > {
                    it.occurredAt
                }.thenBy {
                    it.journalId.value
                }
            )
    }

    override fun snapshot(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,
    ):
        ExternalScheduleSyncJournalSnapshot {

        return ExternalScheduleSyncJournalSnapshot(

            scheduleId =
                scheduleId,

            runId =
                runId,

            events =
                findByRun(runId)
                    .filter {
                        it.scheduleId == scheduleId
                    },
        )
    }
}
