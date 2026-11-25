package me.moiclathy.dism.external.schedule

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExternalScheduleEntryTest {

    private val start =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val end =
        Instant.parse(
            "2026-09-16T09:40:00Z"
        )

    @Test
    fun entryShouldStoreBasicFields() {

        val entry =
            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "高等数学",

                source =
                    ExternalScheduleOrigin.XIQUEER,

                startAt =
                    start,

                endAt =
                    end,

                location =
                    "A教学楼301",

                instructor =
                    "张老师",

                courseCode =
                    "MATH101",
            )

        assertEquals(
            "高等数学",
            entry.title,
        )

        assertEquals(
            ExternalScheduleOrigin.XIQUEER,
            entry.source,
        )

        assertEquals(
            start,
            entry.startAt,
        )

        assertEquals(
            end,
            entry.endAt,
        )

        assertEquals(
            "A教学楼301",
            entry.location,
        )

        assertEquals(
            "张老师",
            entry.instructor,
        )

        assertEquals(
            "MATH101",
            entry.courseCode,
        )
    }

    @Test
    fun completeIntervalShouldBeDetected() {

        val entry =
            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "高等数学",

                source =
                    ExternalScheduleOrigin.XIQUEER,

                startAt =
                    start,

                endAt =
                    end,
            )

        assertTrue(
            entry.hasInterval
        )

        assertTrue(
            entry.hasStartTime
        )
    }

    @Test
    fun missingEndTimeShouldBeAllowed() {

        val entry =
            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "待解析课程",

                source =
                    ExternalScheduleOrigin.FILE,

                startAt =
                    start,
            )

        assertTrue(
            entry.hasStartTime
        )

        assertFalse(
            entry.hasInterval
        )
    }

    @Test
    fun missingTimeShouldBeAllowed() {

        val entry =
            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "未解析课程",

                source =
                    ExternalScheduleOrigin.FILE,
            )

        assertFalse(
            entry.hasStartTime
        )

        assertFalse(
            entry.hasInterval
        )
    }

    @Test
    fun startAfterEndShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "错误课程",

                source =
                    ExternalScheduleOrigin.FILE,

                startAt =
                    end,

                endAt =
                    start,
            )
        }
    }

    @Test
    fun blankTitleShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    " ",

                source =
                    ExternalScheduleOrigin.FILE,
            )
        }
    }

    @Test
    fun metadataShouldBeAvailable() {

        val metadata =
            ExternalScheduleMetadata(
                mapOf(
                    "rawType" to "class",
                    "weekText" to "1-16",
                )
            )

        val entry =
            ExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "course-001"
                    ),

                title =
                    "高等数学",

                source =
                    ExternalScheduleOrigin.XIQUEER,

                metadata =
                    metadata,
            )

        assertEquals(
            "class",
            entry.metadata["rawType"],
        )

        assertTrue(
            "weekText" in entry.metadata
        )
    }
}
