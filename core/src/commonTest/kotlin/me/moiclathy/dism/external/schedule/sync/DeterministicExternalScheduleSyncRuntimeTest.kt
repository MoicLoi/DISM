package me.moiclathy.dism.external.schedule.sync

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncRuntimeTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val time =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private fun record(
        key: String,
        title: String = "实验动物学",
    ): ExternalSchedulePersistenceRecord {

        return ExternalSchedulePersistenceRecord(

            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                "event-$key",

            title =
                title,

            startAt =
                time,

            endAt =
                Instant.parse(
                    "2026-09-10T02:00:00Z"
                ),

            locationEntityId =
                "location-001",

            instructorEntityId =
                "teacher-001",

            courseEntityId =
                "course-001",

            lastSeenAt =
                time,

            active =
                true,

            fingerprint =
                "fp-$key",
        )
    }

    private class FakeHandler(
        private val failCreate: Boolean = false,
        private val failUpdate: Boolean = false,
        private val failRemoval: Boolean = false,
    ) : ExternalScheduleSyncHandler {

        val created =
            mutableListOf<String>()

        val updated =
            mutableListOf<String>()

        val removed =
            mutableListOf<String>()

        override fun create(
            change: ExternalScheduleSyncChange,
        ): String? {

            if (failCreate) {
                error("create failure")
            }

            created +=
                change.externalKey

            return "created-${change.externalKey}"
        }

        override fun update(
            change: ExternalScheduleSyncChange,
        ): String? {

            if (failUpdate) {
                error("update failure")
            }

            updated +=
                change.externalKey

            return "updated-${change.externalKey}"
        }

        override fun handleRemoved(
            change: ExternalScheduleSyncChange,
        ): String? {

            if (failRemoval) {
                error("remove failure")
            }

            removed +=
                change.externalKey

            return "removed-${change.externalKey}"
        }
    }

    @Test
    fun `create action is executed`() {

        val handler =
            FakeHandler()

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val change =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .CREATE_EVENT,

                diff =
                    createAddedDiff(),
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    listOf(change),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .COMPLETED,

            result.status
        )

        assertEquals(
            listOf("entry-001"),
            handler.created
        )

        assertEquals(
            "created-entry-001",
            result.executions
                .single()
                .eventId
        )
    }

    @Test
    fun `update action is executed`() {

        val handler =
            FakeHandler()

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val change =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .UPDATE_EVENT,

                diff =
                    createChangedDiff(),
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    listOf(change),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .COMPLETED,

            result.status
        )

        assertEquals(
            listOf("entry-001"),
            handler.updated
        )
    }

    @Test
    fun `removed action is executed`() {

        val handler =
            FakeHandler()

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val change =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .HANDLE_REMOVED_EVENT,

                diff =
                    createRemovedDiff(),
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    listOf(change),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .COMPLETED,

            result.status
        )

        assertEquals(
            listOf("entry-001"),
            handler.removed
        )
    }

    @Test
    fun `no op is skipped`() {

        val handler =
            FakeHandler()

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val change =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .NO_OP,

                diff =
                    createUnchangedDiff(),
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    listOf(change),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .COMPLETED,

            result.status
        )

        assertEquals(
            1,
            result.skippedCount
        )

        assertTrue(
            handler.created.isEmpty()
        )

        assertTrue(
            handler.updated.isEmpty()
        )

        assertTrue(
            handler.removed.isEmpty()
        )
    }

    @Test
    fun `single failure produces failed status`() {

        val handler =
            FakeHandler(
                failCreate = true
            )

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val change =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .CREATE_EVENT,

                diff =
                    createAddedDiff(),
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    listOf(change),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .FAILED,

            result.status
        )

        assertEquals(
            1,
            result.failedCount
        )

        assertEquals(
            1,
            result.errors.size
        )
    }

    @Test
    fun `one failure does not stop later changes`() {

        val handler =
            object : ExternalScheduleSyncHandler {

                val calls =
                    mutableListOf<String>()

                override fun create(
                    change:
                        ExternalScheduleSyncChange,
                ): String? {

                    calls +=
                        change.externalKey

                    if (
                        change.externalKey ==
                            "entry-001"
                    ) {
                        error("first failure")
                    }

                    return "event-${change.externalKey}"
                }

                override fun update(
                    change:
                        ExternalScheduleSyncChange,
                ): String? {

                    calls +=
                        change.externalKey

                    return "event-${change.externalKey}"
                }

                override fun handleRemoved(
                    change:
                        ExternalScheduleSyncChange,
                ): String? {

                    calls +=
                        change.externalKey

                    return "event-${change.externalKey}"
                }
            }

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val first =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .CREATE_EVENT,

                diff =
                    createAddedDiff(
                        key = "entry-001"
                    ),
            )

        val second =
            ExternalScheduleSyncChange(
                externalKey =
                    "entry-002",

                action =
                    ExternalScheduleSyncAction
                        .CREATE_EVENT,

                diff =
                    createAddedDiff(
                        key = "entry-002"
                    ),
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    listOf(
                        first,
                        second,
                    ),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .PARTIAL,

            result.status
        )

        assertEquals(
            listOf(
                "entry-001",
                "entry-002",
            ),

            handler.calls
        )

        assertEquals(
            1,
            result.failedCount
        )

        assertEquals(
            1,
            result.createdCount
        )
    }

    @Test
    fun `empty plan returns no changes`() {

        val handler =
            FakeHandler()

        val runtime =
            DeterministicExternalScheduleSyncRuntime(
                handler
            )

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    createDiff(),

                changes =
                    emptyList(),
            )

        val result =
            runtime.execute(
                plan
            )

        assertEquals(
            ExternalScheduleSyncRuntimeStatus
                .NO_CHANGES,

            result.status
        )

        assertTrue(
            result.executions.isEmpty()
        )
    }

    private fun createDiff() =
        createAddedDiff().let {
            me.moiclathy.dism.external.schedule.diff
                .ExternalScheduleDiff(
                    scheduleId =
                        scheduleId,

                    records =
                        emptyList(),
                )
        }

    private fun createAddedDiff(
        key: String = "entry-001",
    ) =
        me.moiclathy.dism.external.schedule.diff
            .ExternalScheduleDiffRecord(
                externalKey =
                    key,

                type =
                    me.moiclathy.dism.external.schedule.diff
                        .ExternalScheduleDiffType
                        .ADDED,

                previous =
                    null,

                current =
                    record(key),
            )

    private fun createChangedDiff() =
        me.moiclathy.dism.external.schedule.diff
            .ExternalScheduleDiffRecord(
                externalKey =
                    "entry-001",

                type =
                    me.moiclathy.dism.external.schedule.diff
                        .ExternalScheduleDiffType
                        .CHANGED,

                previous =
                    record(
                        key = "entry-001",
                        title = "旧课程",
                    ),

                current =
                    record(
                        key = "entry-001",
                        title = "新课程",
                    ),
            )

    private fun createRemovedDiff() =
        me.moiclathy.dism.external.schedule.diff
            .ExternalScheduleDiffRecord(
                externalKey =
                    "entry-001",

                type =
                    me.moiclathy.dism.external.schedule.diff
                        .ExternalScheduleDiffType
                        .REMOVED,

                previous =
                    record("entry-001"),

                current =
                    null,
            )

    private fun createUnchangedDiff() =
        me.moiclathy.dism.external.schedule.diff
            .ExternalScheduleDiffRecord(
                externalKey =
                    "entry-001",

                type =
                    me.moiclathy.dism.external.schedule.diff
                        .ExternalScheduleDiffType
                        .UNCHANGED,

                previous =
                    record("entry-001"),

                current =
                    record("entry-001"),
            )
}
