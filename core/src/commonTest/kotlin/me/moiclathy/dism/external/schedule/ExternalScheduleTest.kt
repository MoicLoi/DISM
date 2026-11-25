package me.moiclathy.dism.external.schedule

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExternalScheduleTest {

    private val importedAt =
        Instant.parse(
            "2026-09-16T07:00:00Z"
        )

    private val firstStart =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val secondStart =
        Instant.parse(
            "2026-09-17T10:00:00Z"
        )

    private fun entry(
        id: String,
        title: String,
        start: Instant,
    ) =
        ExternalScheduleEntry(
            id =
                ExternalScheduleId(id),

            title =
                title,

            source =
                ExternalScheduleOrigin.XIQUEER,

            startAt =
                start,
        )

    @Test
    fun scheduleShouldStoreEntries() {

        val first =
            entry(
                id =
                    "course-001",

                title =
                    "高等数学",

                start =
                    firstStart,
            )

        val second =
            entry(
                id =
                    "course-002",

                title =
                    "英语",

                start =
                    secondStart,
            )

        val schedule =
            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "semester-2026"
                    ),

                source =
                    ExternalScheduleOrigin.XIQUEER,

                name =
                    "2026秋季课程表",

                importedAt =
                    importedAt,

                entries =
                    listOf(
                        first,
                        second,
                    ),
            )

        assertEquals(
            2,
            schedule.size,
        )

        assertTrue(
            schedule.isNotEmpty
        )

        assertEquals(
            first,
            schedule.find(
                ExternalScheduleId(
                    "course-001"
                )
            ),
        )
    }

    @Test
    fun duplicateEntryIdsShouldFail() {

        val first =
            entry(
                id =
                    "same",

                title =
                    "高等数学",

                start =
                    firstStart,
            )

        val second =
            entry(
                id =
                    "same",

                title =
                    "英语",

                start =
                    secondStart,
            )

        assertFailsWith<IllegalArgumentException> {

            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "semester-2026"
                    ),

                source =
                    ExternalScheduleOrigin.XIQUEER,

                name =
                    "课程表",

                importedAt =
                    importedAt,

                entries =
                    listOf(
                        first,
                        second,
                    ),
            )
        }
    }

    @Test
    fun differentEntrySourceShouldFail() {

        val entry =
            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "高等数学",

                source =
                    ExternalScheduleOrigin.ICS,
            )

        assertFailsWith<IllegalArgumentException> {

            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "semester-2026"
                    ),

                source =
                    ExternalScheduleOrigin.XIQUEER,

                name =
                    "课程表",

                importedAt =
                    importedAt,

                entries =
                    listOf(entry),
            )
        }
    }

    @Test
    fun sortedEntriesShouldBeDeterministic() {

        val late =
            entry(
                id =
                    "course-002",

                title =
                    "英语",

                start =
                    secondStart,
            )

        val early =
            entry(
                id =
                    "course-001",

                title =
                    "高等数学",

                start =
                    firstStart,
            )

        val schedule =
            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "semester-2026"
                    ),

                source =
                    ExternalScheduleOrigin.XIQUEER,

                name =
                    "课程表",

                importedAt =
                    importedAt,

                entries =
                    listOf(
                        late,
                        early,
                    ),
            )

        assertEquals(
            listOf(
                early,
                late,
            ),
            schedule.sortedByStartTime(),
        )
    }

    @Test
    fun emptyScheduleShouldBeAllowed() {

        val schedule =
            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "empty"
                    ),

                source =
                    ExternalScheduleOrigin.FILE,

                name =
                    "空课程表",

                importedAt =
                    importedAt,

                entries =
                    emptyList(),
            )

        assertTrue(
            schedule.isEmpty
        )

        assertEquals(
            0,
            schedule.size,
        )
    }
}
