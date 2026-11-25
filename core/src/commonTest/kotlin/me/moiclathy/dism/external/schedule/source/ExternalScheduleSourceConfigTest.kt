package me.moiclathy.dism.external.schedule.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalScheduleSourceConfigTest {

    @Test
    fun emptyConfigShouldBeEmpty() {

        val config =
            ExternalScheduleSourceConfig.EMPTY

        assertTrue(
            config.isEmpty
        )

        assertFalse(
            config.isNotEmpty
        )
    }

    @Test
    fun withShouldCreateNewConfig() {

        val original =
            ExternalScheduleSourceConfig.EMPTY

        val updated =
            original.with(
                "format",
                "ics",
            )

        assertTrue(
            original.isEmpty
        )

        assertEquals(
            "ics",
            updated["format"],
        )

        assertTrue(
            updated.isNotEmpty
        )
    }

    @Test
    fun containsShouldWork() {

        val config =
            ExternalScheduleSourceConfig(
                mapOf(
                    "endpoint" to "example",
                )
            )

        assertTrue(
            "endpoint" in config
        )

        assertFalse(
            "token" in config
        )
    }
}
