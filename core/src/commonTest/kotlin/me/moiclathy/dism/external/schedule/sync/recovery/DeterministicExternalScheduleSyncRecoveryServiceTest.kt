package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncRecoveryServiceTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val time =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private val repository =
        FakeRepository()

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

    private fun history(
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
    fun `failed create is recovered by retry create`() {

        repository.records =
            listOf(
                history(
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
            )

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",
                            eventId = null,
                        )
                    ),
            )

        assertTrue(
            result.successful
        )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .RETRY_CREATE,

            record.action
        )
    }

    @Test
    fun `failed update is recovered by retry update`() {

        repository.records =
            listOf(
                history(
                    key = "entry-001",

                    action =
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .FAILED,
                )
            )

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current("entry-001")
                    ),
            )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .RETRY_UPDATE,

            record.action
        )
    }

    @Test
    fun `failed removal is recovered`() {

        repository.records =
            listOf(
                history(
                    key = "entry-001",

                    action =
                        ExternalScheduleSyncAction
                            .HANDLE_REMOVED_EVENT,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .FAILED,
                )
            )

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    emptyList(),
            )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .RETRY_REMOVAL,

            record.action
        )
    }

    @Test
    fun `missing event mapping is recreated`() {

        repository.records =
            emptyList()

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",
                            eventId = null,
                        )
                    ),
            )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .RECREATE_EVENT_MAPPING,

            record.action
        )
    }

    @Test
    fun `successful previous sync is skipped`() {

        repository.records =
            listOf(
                history(
                    key = "entry-001",

                    action =
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .UPDATED,
                )
            )

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current("entry-001")
                    ),
            )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .SKIP,

            record.action
        )
    }

    @Test
    fun `inactive external record is skipped`() {

        repository.records =
            emptyList()

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",
                            active = false,
                        )
                    ),
            )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .SKIP,

            record.action
        )

        assertEquals(
            ExternalScheduleSyncRecoveryReason
                .INACTIVE_EXTERNAL_RECORD,

            record.reason
        )
    }

    @Test
    fun `recreation can be disabled`() {

        repository.records =
            emptyList()

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current(
                            key = "entry-001",
                            eventId = null,
                        )
                    ),

                policy =
                    ExternalScheduleSyncRecoveryPolicy(
                        recreateMissingMapping =
                            false,

                        reconcileMissingMapping =
                            true,
                    ),
            )

        val record =
            result.plan!!
                .records
                .single()

        assertEquals(
            ExternalScheduleSyncRecoveryAction
                .RECONCILE,

            record.action
        )
    }

    @Test
    fun `multiple records produce deterministic plan`() {

        repository.records =
            listOf(
                history(
                    key = "entry-003",

                    action =
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .FAILED,
                ),

                history(
                    key = "entry-001",

                    action =
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,

                    status =
                        ExternalScheduleSyncExecutionStatus
                            .UPDATED,
                ),
            )

        val service =
            DeterministicExternalScheduleSyncRecoveryService(
                repository
            )

        val result =
            service.recover(
                scheduleId =
                    scheduleId,

                currentRecords =
                    listOf(
                        current("entry-003"),

                        current(
                            key = "entry-002",
                            eventId = null,
                        ),

                        current("entry-001"),
                    ),
            )

        val keys =
            result.plan!!
                .records
                .map {
                    it.externalKey
                }

        assertEquals(
            listOf(
                "entry-001",
                "entry-002",
                "entry-003",
            ),

            keys
        )
    }

    private class FakeRepository :
        ExternalScheduleSyncRecoveryRepository {

        var records:
            List<ExternalScheduleSyncPersistenceRecord> =
            emptyList()

        override fun findLatestRun(
            scheduleId:
                ExternalScheduleId,
        ): List<ExternalScheduleSyncPersistenceRecord> {

            return records.filter {
                it.scheduleId == scheduleId
            }
        }

        override fun findLatest(
            scheduleId:
                ExternalScheduleId,

            externalKey:
                String,
        ): ExternalScheduleSyncPersistenceRecord? {

            return records
                .filter {
                    it.scheduleId == scheduleId &&
                        it.externalKey == externalKey
                }
                .maxByOrNull {
                    it.executedAt
                }
        }
    }
}
