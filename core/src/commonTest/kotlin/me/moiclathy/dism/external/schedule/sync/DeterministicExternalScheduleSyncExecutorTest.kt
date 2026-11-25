package me.moiclathy.dism.external.schedule.sync

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncExecutorTest {

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
        active: Boolean = true,
        fingerprint: String? = "fp-$key",
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
                active,

            fingerprint =
                fingerprint,
        )
    }

    private val executor =
        DeterministicExternalScheduleSyncExecutor()

    @Test
    fun `added record creates event action`() {

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous = emptyList(),

                current =
                    listOf(
                        record("entry-001")
                    ),
            )

        assertTrue(
            result.successful
        )

        val plan =
            result.plan!!

        assertEquals(
            1,
            plan.createCount
        )

        assertEquals(
            ExternalScheduleSyncAction.CREATE_EVENT,
            plan.changes.single().action
        )
    }

    @Test
    fun `changed record creates update action`() {

        val previous =
            record(
                key = "entry-001",
                title = "旧课程",
            )

        val current =
            record(
                key = "entry-001",
                title = "新课程",
            )

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(previous),

                current =
                    listOf(current),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.plan!!
                .updateCount
        )

        assertEquals(
            ExternalScheduleSyncAction.UPDATE_EVENT,
            result.plan!!
                .updates
                .single()
                .action
        )
    }

    @Test
    fun `removed record creates removal handling action`() {

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(
                        record("entry-001")
                    ),

                current = emptyList(),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.plan!!
                .removalCount
        )

        assertEquals(
            ExternalScheduleSyncAction
                .HANDLE_REMOVED_EVENT,

            result.plan!!
                .removals
                .single()
                .action
        )
    }

    @Test
    fun `unchanged record creates no op`() {

        val previous =
            record("entry-001")

        val current =
            previous.copy()

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(previous),

                current =
                    listOf(current),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.plan!!
                .noOpCount
        )

        assertEquals(
            ExternalScheduleSyncAction.NO_OP,
            result.plan!!
                .noOps
                .single()
                .action
        )
    }

    @Test
    fun `no op can be disabled`() {

        val previous =
            record("entry-001")

        val current =
            previous.copy()

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(previous),

                current =
                    listOf(current),

                policy =
                    ExternalScheduleSyncPolicy(
                        includeNoOp = false
                    ),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            0,
            result.plan!!
                .noOpCount
        )
    }

    @Test
    fun `creation can be disabled`() {

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous = emptyList(),

                current =
                    listOf(
                        record("entry-001")
                    ),

                policy =
                    ExternalScheduleSyncPolicy(
                        allowCreate = false
                    ),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            0,
            result.plan!!
                .createCount
        )

        assertTrue(
            result.warnings.isNotEmpty()
        )
    }

    @Test
    fun `update can be disabled`() {

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(
                        record(
                            "entry-001",
                            title = "旧课程",
                        )
                    ),

                current =
                    listOf(
                        record(
                            "entry-001",
                            title = "新课程",
                        )
                    ),

                policy =
                    ExternalScheduleSyncPolicy(
                        allowUpdate = false
                    ),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            0,
            result.plan!!
                .updateCount
        )

        assertTrue(
            result.warnings.isNotEmpty()
        )
    }

    @Test
    fun `removal handling can be disabled`() {

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(
                        record("entry-001")
                    ),

                current = emptyList(),

                policy =
                    ExternalScheduleSyncPolicy(
                        allowRemovalHandling = false
                    ),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            0,
            result.plan!!
                .removalCount
        )

        assertTrue(
            result.warnings.isNotEmpty()
        )
    }

    @Test
    fun `mixed schedule produces complete sync plan`() {

        val previous =
            listOf(
                record("unchanged"),

                record(
                    key = "changed",
                    title = "旧课程",
                ),

                record("removed"),
            )

        val current =
            listOf(
                record("unchanged"),

                record(
                    key = "changed",
                    title = "新课程",
                ),

                record("added"),
            )

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    previous,

                current =
                    current,
            )

        assertTrue(
            result.successful
        )

        val plan =
            result.plan!!

        assertEquals(
            1,
            plan.createCount
        )

        assertEquals(
            1,
            plan.updateCount
        )

        assertEquals(
            1,
            plan.removalCount
        )

        assertEquals(
            1,
            plan.noOpCount
        )
    }

    @Test
    fun `event id is preserved in update diff`() {

        val previous =
            record(
                key = "entry-001"
            )

        val current =
            record(
                key = "entry-001",
                title = "课程名称更新",
            )

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous =
                    listOf(previous),

                current =
                    listOf(current),
            )

        val change =
            result.plan!!
                .updates
                .single()

        assertEquals(
            "event-entry-001",
            change.diff
                .previous!!
                .eventId
        )

        assertEquals(
            "event-entry-001",
            change.diff
                .current!!
                .eventId
        )
    }

    @Test
    fun `invalid duplicate keys fail sync planning`() {

        val result =
            executor.plan(
                scheduleId = scheduleId,

                previous = emptyList(),

                current =
                    listOf(
                        record("entry-001"),
                        record("entry-001"),
                    ),
            )

        assertFalse(
            result.successful
        )

        assertTrue(
            result.errors.isNotEmpty()
        )
    }

    @Test
    fun `sync planning is deterministic`() {

        val previous =
            listOf(
                record("entry-003"),
                record("entry-001"),
                record("entry-002"),
            )

        val current =
            listOf(
                record(
                    "entry-002",
                    title = "修改课程",
                ),

                record("entry-004"),

                record("entry-001"),
            )

        val first =
            executor.plan(
                scheduleId,
                previous,
                current,
            )

        val second =
            executor.plan(
                scheduleId,
                previous,
                current,
            )

        assertEquals(
            first,
            second
        )
    }
}
