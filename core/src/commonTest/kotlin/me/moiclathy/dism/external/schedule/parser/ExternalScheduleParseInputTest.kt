package me.moiclathy.dism.external.schedule.parser

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExternalScheduleParseInputTest {

    @Test
    fun inputShouldStoreRawContent() {

        val input =
            ExternalScheduleParseInput(
                sourceId =
                    ExternalScheduleSourceId(
                        "xiqueer-main"
                    ),

                origin =
                    ExternalScheduleOrigin.XIQUEER,

                rawContent =
                    "raw schedule data",

                metadata =
                    mapOf(
                        "format" to "text",
                    ),
            )

        assertEquals(
            "raw schedule data",
            input.rawContent,
        )

        assertEquals(
            "text",
            input["format"],
        )

        assertTrue(
            "format" in input
        )
    }

    @Test
    fun blankRawContentShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleParseInput(
                sourceId =
                    ExternalScheduleSourceId(
                        "test"
                    ),

                origin =
                    ExternalScheduleOrigin.FILE,

                rawContent =
                    " ",
            )
        }
    }

    @Test
    fun requestedMetadataShouldBeIndependent() {

        val input =
            ExternalScheduleParseInput(
                sourceId =
                    ExternalScheduleSourceId(
                        "test"
                    ),

                origin =
                    ExternalScheduleOrigin.FILE,

                rawContent =
                    "data",

                metadata =
                    mapOf(
                        "requestedAt" to
                            Instant.parse(
                                "2026-09-16T08:00:00Z"
                            ).toString(),
                    ),
            )

        assertEquals(
            "2026-09-16T08:00:00Z",
            input["requestedAt"],
        )
    }
}
