package me.moiclathy.dism.external.schedule

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalScheduleFactoryTest {

    private val start =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val end =
        Instant.parse(
            "2026-09-16T09:40:00Z"
        )

    private val importedAt =
        Instant.parse(
            "2026-09-16T07:00:00Z"
        )

    @Test
    fun factoryShouldCreateEntry() {

        val entry =
            ExternalScheduleFactory.createEntry(
                id =
                    "xq-001",

                title =
                    "实验动物学",

                source =
                    ExternalScheduleOrigin.XIQUEER,

                startAt =
                    start,

                endAt =
                    end,

                location =
                    "实验楼201",

                instructor =
                    "李老师",

                courseCode =
                    "LAB001",
            )

        assertEquals(
            "xq-001",
            entry.id.value,
        )

        assertEquals(
            "实验动物学",
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
    }

    @Test
    fun factoryShouldCreateSchedule() {

        val entry =
            ExternalScheduleFactory.createEntry(
                id =
                    "xq-001",

                title =
                    "实验动物学",

                source =
                    ExternalScheduleOrigin.XIQUEER,

                startAt =
                    start,

                endAt =
                    end,
            )

        val schedule =
            ExternalScheduleFactory.createSchedule(
                id =
                    "semester-2026",

                source =
                    ExternalScheduleOrigin.XIQUEER,

                name =
                    "2026秋季课程表",

                importedAt =
                    importedAt,

                entries =
                    listOf(entry),
            )

        assertEquals(
            "semester-2026",
            schedule.id.value,
        )

        assertEquals(
            1,
            schedule.size,
        )

        assertEquals(
            entry,
            schedule.entries.first(),
        )
    }
}
