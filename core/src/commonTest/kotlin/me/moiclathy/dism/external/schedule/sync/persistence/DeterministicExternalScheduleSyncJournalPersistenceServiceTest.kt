package me.moiclathy.dism.external.schedule.sync.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class DeterministicExternalScheduleSyncJournalPersistenceServiceTest {
    private val scheduleId = ExternalScheduleId("schedule-001")
    private val runId = ExternalScheduleSyncRunId("run-001")
    private val time = Instant.parse("2026-09-18T10:00:00Z")

    private fun event(
        id: String,
        type: ExternalScheduleSyncJournalEventType,
        key: String? = null,
        action: ExternalScheduleSyncAction? = null,
        eventId: String? = null,
        message: String? = null,
    ) = ExternalScheduleSyncJournalEvent(
        journalId = ExternalScheduleSyncJournalId(id),
        scheduleId = scheduleId,
        runId = runId,
        type = type,
        occurredAt = time,
        externalKey = key,
        action = action,
        eventId = eventId,
        message = message,
    )

    @Test
    fun `domain event round trips through persistence`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        val original = event(
            id = "journal-1",
            type = ExternalScheduleSyncJournalEventType.EVENT_CREATED,
            key = "course-a",
            action = ExternalScheduleSyncAction.CREATE_EVENT,
            eventId = "event-a",
        )

        val result = service.append(original)
        val restored = service.find(original.journalId)

        assertTrue(result.successful)
        assertEquals(1, result.affectedRecords)
        assertEquals(original, restored)
    }

    @Test
    fun `duplicate journal id is idempotent`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        val original = event("journal-1", ExternalScheduleSyncJournalEventType.RUN_STARTED)
        val replacement = event("journal-1", ExternalScheduleSyncJournalEventType.RUN_COMPLETED)

        service.append(original)
        service.append(replacement)

        val restored = service.find(original.journalId)
        assertEquals(ExternalScheduleSyncJournalEventType.RUN_STARTED, restored?.type)
    }

    @Test
    fun `run can be restored in deterministic order`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        val later = event("b", ExternalScheduleSyncJournalEventType.RUN_COMPLETED)
        val earlier = later.copy(journalId = ExternalScheduleSyncJournalId("a"), occurredAt = Instant.parse("2026-09-18T09:00:00Z"))

        service.appendAll(listOf(later, earlier))
        val restored = service.restoreRun(runId)

        assertEquals(listOf("a", "b"), restored.map { it.journalId.value })
    }

    @Test
    fun `append all empty is successful`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        val result = service.appendAll(emptyList())

        assertTrue(result.successful)
        assertEquals(0, result.affectedRecords)
    }

    @Test
    fun `snapshot filters by schedule and run`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        val first = event("a", ExternalScheduleSyncJournalEventType.RUN_STARTED)
        service.append(first)

        val snapshot = repository.snapshot(scheduleId, runId)
        assertEquals(1, snapshot.size)
        assertNotNull(snapshot.records.single())
    }

    @Test
    fun `delete run removes only matching run`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        service.append(event("a", ExternalScheduleSyncJournalEventType.RUN_STARTED))

        val otherRun = ExternalScheduleSyncRunId("run-002")
        val other = event("b", ExternalScheduleSyncJournalEventType.RUN_STARTED)
            .copy(runId = otherRun)
        repository.append(ExternalScheduleSyncJournalPersistenceRecord.fromDomain(other))

        val result = service.deleteRun(runId)

        assertEquals(1, result.affectedRecords)
        assertEquals(null, service.find(ExternalScheduleSyncJournalId("a")))
        assertNotNull(service.find(ExternalScheduleSyncJournalId("b")))
    }

    @Test
    fun `find by type restores only requested facts`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = ExternalScheduleSyncJournalPersistenceService(repository)
        service.append(event("a", ExternalScheduleSyncJournalEventType.RUN_STARTED))
        service.append(event("b", ExternalScheduleSyncJournalEventType.RUN_COMPLETED))

        val result = service.findByType(runId, ExternalScheduleSyncJournalEventType.RUN_COMPLETED)
        assertEquals(1, result.size)
        assertEquals("b", result.single().journalId.value)
    }
}
