package me.moiclathy.dism.external.schedule.normalization

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NormalizedExternalScheduleEntryTest {

    @Test
    fun entryShouldStoreNormalizedFields() {

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
                    "张老师",

                courseCode =
                    "MATH101",

                description =
                    null,
            )

        assertEquals(
            "高等数学",
            entry.title,
        )

        assertEquals(
            "A301",
            entry.location,
        )

        assertEquals(
            "MATH101",
            entry.courseCode,
        )
    }

    @Test
    fun blankTitleShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            NormalizedExternalScheduleEntry(
                id =
                    ExternalScheduleId(
                        "entry"
                    ),

                title =
                    " ",

                startAt =
                    null,

                endAt =
                    null,

                location =
                    null,

                instructor =
                    null,

                courseCode =
                    null,

                description =
                    null,
            )
        }
    }
}
