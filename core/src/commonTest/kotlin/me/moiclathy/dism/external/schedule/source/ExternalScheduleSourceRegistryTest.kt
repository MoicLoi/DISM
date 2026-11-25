package me.moiclathy.dism.external.schedule.source

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExternalScheduleSourceRegistryTest {

    private val time =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private fun source(
        id: String,
    ): ExternalScheduleSource {

        val schedule =
            ExternalSchedule(
                id =
                    ExternalScheduleId(
                        "schedule-$id"
                    ),

                source =
                    ExternalScheduleOrigin.FILE,

                name =
                    "Schedule $id",

                importedAt =
                    time,

                entries =
                    emptyList(),
            )

        return StaticExternalScheduleSource(
            id =
                ExternalScheduleSourceId(id),

            schedule =
                schedule,
        )
    }

    @Test
    fun sourceShouldBeRegistered() {

        val registry =
            ExternalScheduleSourceRegistry()

        val source =
            source("file-main")

        registry.register(
            source
        )

        assertEquals(
            1,
            registry.size,
        )

        assertTrue(
            registry.contains(
                ExternalScheduleSourceId(
                    "file-main"
                )
            )
        )

        assertNotNull(
            registry.find(
                ExternalScheduleSourceId(
                    "file-main"
                )
            )
        )
    }

    @Test
    fun duplicateSourceShouldFail() {

        val registry =
            ExternalScheduleSourceRegistry()

        registry.register(
            source("file-main")
        )

        assertFailsWith<IllegalArgumentException> {

            registry.register(
                source("file-main")
            )
        }
    }

    @Test
    fun registerAllShouldRegisterSources() {

        val registry =
            ExternalScheduleSourceRegistry()

        registry.registerAll(
            listOf(
                source("a"),
                source("b"),
                source("c"),
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
            ExternalScheduleSourceRegistry()

        registry.register(
            source("file-main")
        )

        assertTrue(
            registry.remove(
                ExternalScheduleSourceId(
                    "file-main"
                )
            )
        )

        assertEquals(
            0,
            registry.size,
        )
    }
}
