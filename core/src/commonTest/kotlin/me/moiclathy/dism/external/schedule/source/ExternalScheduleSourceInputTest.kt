package me.moiclathy.dism.external.schedule.source

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExternalScheduleSourceInputTest {

    @Test
    fun inputShouldStoreRequestTime() {

        val requestedAt =
            Instant.parse(
                "2026-09-16T08:00:00Z"
            )

        val input =
            ExternalScheduleSourceInput(
                requestedAt =
                    requestedAt,

                config =
                    ExternalScheduleSourceConfig(
                        mapOf(
                            "format" to "ics",
                        )
                    ),

                metadata =
                    mapOf(
                        "reason" to "manual-refresh",
                    ),
            )

        assertEquals(
            requestedAt,
            input.requestedAt,
        )

        assertEquals(
            "ics",
            input.config["format"],
        )

        assertEquals(
            "manual-refresh",
            input["reason"],
        )

        assertTrue(
            "reason" in input
        )
    }
}
