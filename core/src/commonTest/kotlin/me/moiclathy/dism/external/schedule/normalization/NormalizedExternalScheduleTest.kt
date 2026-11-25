package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizedExternalScheduleTest {

    @Test
    fun scheduleShouldStoreEntries() {

        val entry =
            NormalizedExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "entry-001"
                    ),

                title =
                    "高等数学",

                startAt =
                    null,

                endAt =
                    null,

                location =
                    "A301",

                instructor =
                    null,

                courseCode =
                    null,

                description =
                    null,
            )

        val schedule =
            NormalizedExternalSchedule(
                id =
                    ExternalScheduleId(
                        "schedule-001"
                    ),

                source =
                    ExternalScheduleOrigin.ICS,

                name =
                    "课程表",

                normalizedAt =
                    Instant.parse(
                        "2026-09-16T08:00:00Z"
                    ),

                entries =
                    listOf(
                        entry
                    ),
            )

        assertEquals(
            1,
            schedule.size,
        )

        assertEquals(
            entry,
            schedule.entry(
                ExternalScheduleId(
                    "entry-001"
                )
            ),
        )
    }
}
