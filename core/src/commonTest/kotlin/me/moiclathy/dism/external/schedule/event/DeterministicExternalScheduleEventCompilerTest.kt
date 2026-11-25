package me.moiclathy.dism.external.schedule.event

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleMetadata
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

class DeterministicExternalScheduleEventCompilerTest {

    private val compiler =
        DeterministicExternalScheduleEventCompiler()

    private val start =
        Instant.parse("2026-09-14T01:00:00Z")

    private val end =
        Instant.parse("2026-09-14T02:30:00Z")

    private fun createEntry(
        id: String = "course-001",
        title: String = "实验动物学",
        startAt: Instant? = start,
        endAt: Instant? = end,
    ): ExternalScheduleEntry {

        return ExternalScheduleEntry(
            id = ExternalScheduleId(id),
            title = title,
            source = ExternalScheduleOrigin.ICS,
            startAt = startAt,
            endAt = endAt,
            location = "实验楼 201",
            instructor = "张老师",
            courseCode = "LAB101",
            description = "实验动物学课程",
            metadata = ExternalScheduleMetadata(
                mapOf(
                    "semester" to "2026-2027-1",
                    "source" to "ics",
                )
            ),
        )
    }

    @Test
    fun `compile entry`() {

        val result =
            compiler.compileEntry(
                createEntry()
            )

        assertTrue(result.successful)
        assertEquals(1, result.drafts.size)
        assertTrue(result.errors.isEmpty())

        val draft =
            result.drafts.single()

        assertEquals("course-001", draft.sourceEntryId.value)
        assertEquals("实验动物学", draft.title)

        assertEquals(
            start,
            draft.startAt
        )

        assertEquals(
            end,
            draft.endAt
        )

        assertEquals(
            "实验楼 201",
            draft.location
        )

        assertEquals(
            "张老师",
            draft.instructor
        )

        assertEquals(
            "LAB101",
            draft.courseCode
        )
    }

    @Test
    fun `external key is deterministic`() {

        val entry = createEntry()

        val first =
            compiler.compileEntry(entry)
                .drafts
                .single()

        val second =
            compiler.compileEntry(entry)
                .drafts
                .single()

        assertEquals(
            first.externalKey,
            second.externalKey
        )
    }

    @Test
    fun `different ids produce different external keys`() {

        val first =
            compiler.compileEntry(
                createEntry(id = "course-001")
            )
                .drafts
                .single()

        val second =
            compiler.compileEntry(
                createEntry(id = "course-002")
            )
                .drafts
                .single()

        assertFalse(
            first.externalKey == second.externalKey
        )
    }

    @Test
    fun `unscheduled entry is allowed by default`() {

        val result =
            compiler.compileEntry(
                createEntry(
                    startAt = null,
                    endAt = null,
                )
            )

        assertTrue(result.successful)

        val draft =
            result.drafts.single()

        assertFalse(
            draft.hasTemporalInformation
        )

        assertFalse(
            draft.hasTimeRange
        )
    }

    @Test
    fun `unscheduled entry can be rejected`() {

        val result =
            compiler.compileEntry(
                createEntry(
                    startAt = null,
                    endAt = null,
                ),
                ExternalScheduleEventCompilationPolicy(
                    allowUnscheduled = false,
                )
            )

        assertFalse(result.successful)

        assertTrue(
            result.errors.any {
                it.code ==
                    ExternalScheduleEventCompilationError.Code
                        .UNSCHEDULED_NOT_ALLOWED
            }
        )
    }

    @Test
    fun `invalid time range is rejected by the entry model`() {

        /*
         * ExternalScheduleEntry 自身已经保证
         * startAt 不晚于 endAt。
         *
         * 因此默认策略下不可能出现
         * 进入 Compiler 的非法时间区间。
         */
        assertFailsWith<IllegalArgumentException> {
            createEntry(
                startAt = end,
                endAt = start,
            )
        }
    }

    @Test
    fun `blank title is rejected by the entry model`() {

        /*
         * 同上：title 为空白时
         * ExternalScheduleEntry 会直接拒绝。
         *
         * Compiler 的 fallback 只会在
         * 上游放开该不变量后才可能触发。
         */
        assertFailsWith<IllegalArgumentException> {
            createEntry(
                title = "   "
            )
        }
    }

    @Test
    fun `metadata is preserved`() {

        val result =
            compiler.compileEntry(
                createEntry()
            )

        val draft =
            result.drafts.single()

        assertEquals(
            "2026-2027-1",
            draft.metadata["semester"]
        )

        assertEquals(
            "ics",
            draft.metadata["source"]
        )
    }

    @Test
    fun `compile complete schedule`() {

        val schedule =
            ExternalSchedule(
                id = ExternalScheduleId("schedule-001"),
                source = ExternalScheduleOrigin.ICS,
                name = "2026 秋季课程表",
                importedAt =
                    Instant.parse("2026-09-01T00:00:00Z"),
                entries = listOf(
                    createEntry("course-001"),
                    createEntry("course-002"),
                    createEntry("course-003"),
                ),
            )

        val result =
            compiler.compile(schedule)

        assertTrue(result.successful)

        assertEquals(
            3,
            result.drafts.size
        )

        assertEquals(
            3,
            result.drafts
                .map { it.externalKey }
                .distinct()
                .size
        )
    }
}
