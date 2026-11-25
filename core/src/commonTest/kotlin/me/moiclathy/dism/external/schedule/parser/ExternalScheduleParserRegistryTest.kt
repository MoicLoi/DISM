package me.moiclathy.dism.external.schedule.parser

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExternalScheduleParserRegistryTest {

    private val time =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private fun parser(
        origin: ExternalScheduleOrigin,
    ): ExternalScheduleParser {

        val schedule =
            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "schedule-${origin.name}"
                    ),

                source =
                    origin,

                name =
                    "Test",

                importedAt =
                    time,

                entries =
                    emptyList(),
            )

        return StaticExternalScheduleParser(
            schedule =
                schedule,
        )
    }

    @Test
    fun parserShouldBeRegistered() {

        val registry =
            ExternalScheduleParserRegistry()

        val parser =
            parser(
                ExternalScheduleOrigin.FILE
            )

        registry.register(
            parser
        )

        assertEquals(
            1,
            registry.size,
        )

        assertTrue(
            registry.contains(
                ExternalScheduleOrigin.FILE
            )
        )

        assertNotNull(
            registry.find(
                ExternalScheduleOrigin.FILE
            )
        )
    }

    @Test
    fun duplicateOriginShouldFail() {

        val registry =
            ExternalScheduleParserRegistry()

        registry.register(
            parser(
                ExternalScheduleOrigin.FILE
            )
        )

        assertFailsWith<IllegalArgumentException> {

            registry.register(
                parser(
                    ExternalScheduleOrigin.FILE
                )
            )
        }
    }

    @Test
    fun registerAllShouldWork() {

        val registry =
            ExternalScheduleParserRegistry()

        registry.registerAll(
            listOf(
                parser(
                    ExternalScheduleOrigin.FILE
                ),

                parser(
                    ExternalScheduleOrigin.ICS
                ),

                parser(
                    ExternalScheduleOrigin.XIQUEER
                ),
            )
        )

        assertEquals(
            3,
            registry.size,
        )
    }

    @Test
    fun removeShouldWork() {

        val registry =
            ExternalScheduleParserRegistry()

        registry.register(
            parser(
                ExternalScheduleOrigin.ICS
            )
        )

        assertTrue(
            registry.remove(
                ExternalScheduleOrigin.ICS
            )
        )

        assertEquals(
            0,
            registry.size,
        )
    }
}
