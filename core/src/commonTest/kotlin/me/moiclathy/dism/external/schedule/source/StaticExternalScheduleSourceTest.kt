package me.moiclathy.dism.external.schedule.source

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticExternalScheduleSourceTest {

    private val time =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val schedule =
        ExternalSchedule(
            id =
                ExternalScheduleId(
                    "schedule-001"
                ),

            source =
                ExternalScheduleOrigin.FILE,

            name =
                "测试课程表",

            importedAt =
                time,

            entries =
                emptyList(),
        )

    @Test
    fun staticSourceShouldReturnSchedule() {

        val source =
            StaticExternalScheduleSource(
                id =
                    ExternalScheduleSourceId(
                        "fixture"
                    ),

                schedule =
                    schedule,
            )

        val result =
            source.fetch(
                ExternalScheduleSourceInput(
                    requestedAt =
                        time,
                )
            )

        assertTrue(
            result.success
        )

        assertEquals(
            schedule,
            result.schedule,
        )

        assertEquals(
            ExternalScheduleSourceId(
                "fixture"
            ),
            result.sourceId,
        )

        assertEquals(
            ExternalScheduleOrigin.FILE,
            source.origin,
        )
    }

    @Test
    fun repeatedFetchShouldBeDeterministic() {

        val source =
            StaticExternalScheduleSource(
                id =
                    ExternalScheduleSourceId(
                        "fixture"
                    ),

                schedule =
                    schedule,
            )

        val input =
            ExternalScheduleSourceInput(
                requestedAt =
                    time,
            )

        val first =
            source.fetch(input)

        val second =
            source.fetch(input)

        assertEquals(
            first,
            second,
        )
    }
}
