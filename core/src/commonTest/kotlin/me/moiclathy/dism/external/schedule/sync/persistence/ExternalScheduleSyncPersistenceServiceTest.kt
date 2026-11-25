package me.moiclathy.dism.external.schedule.sync.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiff
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffRecord
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffType
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncChange
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncPlan
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncRuntimeResult
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncRuntimeStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExternalScheduleSyncPersistenceServiceTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val startedAt =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private val completedAt =
        Instant.parse(
            "2026-09-10T01:00:05Z"
        )

    private fun plan(
        changes:
            List<ExternalScheduleSyncChange>,
    ): ExternalScheduleSyncPlan {

        return ExternalScheduleSyncPlan(

            scheduleId =
                scheduleId,

            diff =
                ExternalScheduleDiff(
                    scheduleId =
                        scheduleId,

                    records =
                        emptyList(),
                ),

            changes =
                changes,
        )
    }

    private fun runtimeResult(
        changes:
            List<ExternalScheduleSyncChange>,
    ): ExternalScheduleSyncRuntimeResult {

        val executions =
            changes.map {
                change ->

                me.moiclathy.dism.external.schedule.sync
                    .ExternalScheduleSyncExecutionRecord(

                        externalKey =
                            change.externalKey,

                        action =
                            change.action,

                        status =
                            when (
                                change.action
                            ) {

                                ExternalScheduleSyncAction
                                    .CREATE_EVENT ->
                                    ExternalScheduleSyncExecutionStatus
                                        .CREATED

                                ExternalScheduleSyncAction
                                    .UPDATE_EVENT ->
                                    ExternalScheduleSyncExecutionStatus
                                        .UPDATED

                                ExternalScheduleSyncAction
                                    .HANDLE_REMOVED_EVENT ->
                                    ExternalScheduleSyncExecutionStatus
                                        .REMOVED

                                ExternalScheduleSyncAction
                                    .RECREATE_EVENT_MAPPING ->
                                    ExternalScheduleSyncExecutionStatus
                                        .UPDATED

                                ExternalScheduleSyncAction
                                    .NO_OP ->
                                    ExternalScheduleSyncExecutionStatus
                                        .SKIPPED
                            },

                        eventId =
                            "event-${change.externalKey}",
                    )
            }

        return ExternalScheduleSyncRuntimeResult(

            status =
                ExternalScheduleSyncRuntimeStatus
                    .COMPLETED,

            plan =
                plan(changes),

            executions =
                executions,
        )
    }

    /**
     * Diff Record 的不变量要求：
     *
     * ADDED   -> current 必须存在
     * REMOVED -> previous 必须存在
     * CHANGED / UNCHANGED -> 两者都必须存在
     *
     * 因此这里按类型提供最小合法 Fixture。
     */
    private fun record(
        key: String,
    ): ExternalSchedulePersistenceRecord {

        return ExternalSchedulePersistenceRecord(

            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                "event-$key",

            title =
                "实验动物学",

            startAt =
                startedAt,

            endAt =
                completedAt,

            lastSeenAt =
                startedAt,

            active =
                true,

            fingerprint =
                "fp-$key",
        )
    }

    private fun change(
        key:
            String,

        action:
            ExternalScheduleSyncAction,
    ): ExternalScheduleSyncChange {

        val diffType =
            when (action) {

                ExternalScheduleSyncAction
                    .CREATE_EVENT ->
                    ExternalScheduleDiffType.ADDED

                ExternalScheduleSyncAction
                    .UPDATE_EVENT ->
                    ExternalScheduleDiffType.CHANGED

                ExternalScheduleSyncAction
                    .HANDLE_REMOVED_EVENT ->
                    ExternalScheduleDiffType.REMOVED

                ExternalScheduleSyncAction
                    .RECREATE_EVENT_MAPPING ->
                    ExternalScheduleDiffType.CHANGED

                ExternalScheduleSyncAction
                    .NO_OP ->
                    ExternalScheduleDiffType.UNCHANGED
            }

        val previous =
            if (
                diffType ==
                ExternalScheduleDiffType.ADDED
            ) {
                null
            } else {
                record(key)
            }

        val current =
            if (
                diffType ==
                ExternalScheduleDiffType.REMOVED
            ) {
                null
            } else {
                record(key)
            }

        return ExternalScheduleSyncChange(

            externalKey =
                key,

            action =
                action,

            diff =
                ExternalScheduleDiffRecord(

                    externalKey =
                        key,

                    type =
                        diffType,

                    previous =
                        previous,

                    current =
                        current,
                ),
        )
    }

    @Test
    fun `persist stores all runtime executions`() {

        val repository =
            InMemoryExternalScheduleSyncPersistenceRepository()

        val service =
            ExternalScheduleSyncPersistenceService(
                repository
            )

        val result =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                    ),

                    change(
                        "entry-002",
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,
                    ),

                    change(
                        "entry-003",
                        ExternalScheduleSyncAction
                            .HANDLE_REMOVED_EVENT,
                    ),

                    change(
                        "entry-004",
                        ExternalScheduleSyncAction
                            .NO_OP,
                    ),
                )
            )

        val persisted =
            service.persist(
                result =
                    result,

                startedAt =
                    startedAt,

                completedAt =
                    completedAt,

                syncRunId =
                    "run-001",
            )

        assertTrue(
            persisted.successful
        )

        assertEquals(
            4,
            persisted.affectedCount
        )

        assertEquals(
            4,
            repository
                .findByRun("run-001")
                .size
        )
    }

    @Test
    fun `persist preserves action and execution status`() {

        val repository =
            InMemoryExternalScheduleSyncPersistenceRepository()

        val service =
            ExternalScheduleSyncPersistenceService(
                repository
            )

        val result =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,
                    )
                )
            )

        service.persist(
            result,
            startedAt,
            completedAt,
            "run-001",
        )

        val record =
            repository
                .findByRun("run-001")
                .single()

        assertEquals(
            ExternalScheduleSyncAction
                .UPDATE_EVENT,

            record.action
        )

        assertEquals(
            ExternalScheduleSyncExecutionStatus
                .UPDATED,

            record.status
        )

        assertEquals(
            "event-entry-001",
            record.eventId
        )
    }

    @Test
    fun `latest record returns newest execution`() {

        val repository =
            InMemoryExternalScheduleSyncPersistenceRepository()

        val service =
            ExternalScheduleSyncPersistenceService(
                repository
            )

        val first =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                    )
                )
            )

        val second =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .UPDATE_EVENT,
                    )
                )
            )

        service.persist(
            first,
            startedAt,
            completedAt,
            "run-001",
        )

        service.persist(
            second,
            startedAt,
            Instant.parse(
                "2026-09-10T01:01:00Z"
            ),
            "run-002",
        )

        val latest =
            service.latestRecord(
                scheduleId =
                    scheduleId,

                externalKey =
                    "entry-001",
            )

        assertEquals(
            ExternalScheduleSyncAction
                .UPDATE_EVENT,

            latest!!.action
        )

        assertEquals(
            "run-002",
            latest.syncRunId
        )
    }

    @Test
    fun `latest run returns complete batch`() {

        val repository =
            InMemoryExternalScheduleSyncPersistenceRepository()

        val service =
            ExternalScheduleSyncPersistenceService(
                repository
            )

        val result =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                    ),

                    change(
                        "entry-002",
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                    ),
                )
            )

        service.persist(
            result,
            startedAt,
            completedAt,
            "run-001",
        )

        val latest =
            service.latestRun(
                scheduleId
            )

        assertEquals(
            2,
            latest.size
        )

        assertTrue(
            latest.all {
                it.syncRunId == "run-001"
            }
        )
    }

    @Test
    fun `blank run id is rejected`() {

        val repository =
            InMemoryExternalScheduleSyncPersistenceRepository()

        val service =
            ExternalScheduleSyncPersistenceService(
                repository
            )

        val result =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                    )
                )
            )

        val persisted =
            service.persist(
                result =
                    result,

                startedAt =
                    startedAt,

                completedAt =
                    completedAt,

                syncRunId =
                    "",
            )

        assertTrue(
            !persisted.successful
        )

        assertEquals(
            1,
            persisted.errors.size
        )
    }

    @Test
    fun `invalid time range is rejected`() {

        val repository =
            InMemoryExternalScheduleSyncPersistenceRepository()

        val service =
            ExternalScheduleSyncPersistenceService(
                repository
            )

        val result =
            runtimeResult(
                listOf(
                    change(
                        "entry-001",
                        ExternalScheduleSyncAction
                            .CREATE_EVENT,
                    )
                )
            )

        val persisted =
            service.persist(
                result =
                    result,

                startedAt =
                    completedAt,

                completedAt =
                    startedAt,

                syncRunId =
                    "run-001",
            )

        assertTrue(
            !persisted.successful
        )
    }
}
