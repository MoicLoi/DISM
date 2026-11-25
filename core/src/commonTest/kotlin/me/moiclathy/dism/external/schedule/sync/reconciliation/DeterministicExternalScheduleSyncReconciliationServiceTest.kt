package me.moiclathy.dism.external.schedule.sync.reconciliation

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncReconciliationServiceTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val time =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private fun current(
        key: String,
        eventId: String? = "event-$key",
        active: Boolean = true,
    ) =
        ExternalSchedulePersistenceRecord(

            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                eventId,

            title =
                "实验动物学",

            startAt =
                time,

            endAt =
                Instant.parse(
                    "2026-09-10T02:00:00Z"
                ),

            locationEntityId =
                "location",

            instructorEntityId =
                "teacher",

            courseEntityId =
                "course",

            lastSeenAt =
                time,

            active =
                active,

            fingerprint =
                "fp-$key",
        )

    private fun sync(
        key: String,
        action: ExternalScheduleSyncAction,
        status: ExternalScheduleSyncExecutionStatus,
        eventId: String? = "event-$key",
    ) =
        ExternalScheduleSyncPersistenceRecord(

            scheduleId =
                scheduleId,

            externalKey =
                key,

            action =
                action,

            status =
                status,

            eventId =
                eventId,

            executedAt =
                time,

            message =
                if (
                    status ==
                        ExternalScheduleSyncExecutionStatus
                            .FAILED
                ) {
                    "failure"
                } else {
                    null
                },

            syncRunId =
                "run-001",
        )

    @Test
    fun `completed sync with existing event is consistent`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    it == "event-entry-001"
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001"
                        )
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "entry-001",

                            action =
                                ExternalScheduleSyncAction
                                    .CREATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .CREATED,
                        )
                    ),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationStatus
                .CONSISTENT,

            record.status
        )

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .NO_OP,

            record.action
        )
    }

    @Test
    fun `in progress equivalent with existing event completes sync`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    it == "event-entry-001"
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001"
                        )
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "entry-001",

                            action =
                                ExternalScheduleSyncAction
                                    .CREATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .FAILED,

                            eventId =
                                "event-entry-001",
                        )
                    ),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationStatus
                .FAILED_BUT_EVENT_EXISTS,

            record.status
        )

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .COMPLETE_SYNC,

            record.action
        )
    }

    @Test
    fun `failed create without event retries create`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    false
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",
                            eventId = null,
                        )
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "entry-001",

                            action =
                                ExternalScheduleSyncAction
                                    .CREATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .FAILED,

                            eventId =
                                null,
                        )
                    ),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .RETRY_CREATE,

            record.action
        )

        assertEquals(
            ExternalScheduleSyncReconciliationStatus
                .FAILED_AND_EVENT_MISSING,

            record.status
        )
    }

    @Test
    fun `failed update with existing event completes sync`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    true
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001"
                        )
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "entry-001",

                            action =
                                ExternalScheduleSyncAction
                                    .UPDATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .FAILED,
                        )
                    ),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .COMPLETE_SYNC,

            record.action
        )
    }

    @Test
    fun `failed update without event retries update`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    false
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001"
                        )
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "entry-001",

                            action =
                                ExternalScheduleSyncAction
                                    .UPDATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .FAILED,
                        )
                    ),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .RETRY_UPDATE,

            record.action
        )
    }

    @Test
    fun `successful sync but deleted event recreates mapping`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    false
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001"
                        )
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "entry-001",

                            action =
                                ExternalScheduleSyncAction
                                    .CREATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .CREATED,
                        )
                    ),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationStatus
                .MISSING_EVENT,

            record.status
        )

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .RETRY_CREATE,

            record.action
        )
    }

    @Test
    fun `active current record without mapping recreates event mapping`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    false
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",
                            eventId = null,
                        )
                    ),

                syncRecords =
                    emptyList(),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .RECREATE_EVENT_MAPPING,

            record.action
        )

        assertEquals(
            ExternalScheduleSyncReconciliationStatus
                .MISSING_EVENT,

            record.status
        )
    }

    @Test
    fun `inactive record requires no action`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    false
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",

                            eventId = null,

                            active = false,
                        )
                    ),

                syncRecords =
                    emptyList(),
            )

        val record =
            result.records.single()

        assertEquals(
            ExternalScheduleSyncReconciliationAction
                .NO_OP,

            record.action
        )

        assertEquals(
            ExternalScheduleSyncReconciliationStatus
                .CONSISTENT,

            record.status
        )
    }

    @Test
    fun `result is deterministic`() {

        val service =
            DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup {
                    it == "event-b"
                }
            )

        val result =
            service.reconcile(

                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "b"
                        ),

                        current(
                            key = "a"
                        ),
                    ),

                syncRecords =
                    listOf(
                        sync(
                            key = "b",

                            action =
                                ExternalScheduleSyncAction
                                    .UPDATE_EVENT,

                            status =
                                ExternalScheduleSyncExecutionStatus
                                    .UPDATED,
                        )
                    ),
            )

        assertEquals(
            listOf(
                "a",
                "b",
            ),

            result.records.map {
                it.externalKey
            }
        )

        assertTrue(
            result.successful
        )
    }
}
