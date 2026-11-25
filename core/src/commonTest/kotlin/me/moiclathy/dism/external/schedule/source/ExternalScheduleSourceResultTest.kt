package me.moiclathy.dism.external.schedule.source

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalScheduleSourceResultTest {

    private val sourceId =
        ExternalScheduleSourceId(
            "test-source"
        )

    private val fetchedAt =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val schedule =
        ExternalSchedule(
            id =
                me.moiclathy.dism.external.schedule.ExternalScheduleId(
                    "schedule-001"
                ),

            source =
                ExternalScheduleOrigin.FILE,

            name =
                "测试课程表",

            importedAt =
                fetchedAt,

            entries =
                emptyList(),
        )

    @Test
    fun successResultShouldContainSchedule() {

        val result =
            ExternalScheduleSourceResult.success(
                sourceId =
                    sourceId,

                schedule =
                    schedule,

                fetchedAt =
                    fetchedAt,
            )

        assertTrue(
            result.success
        )

        assertEquals(
            schedule,
            result.schedule,
        )

        assertNull(
            result.error
        )
    }

    @Test
    fun failureResultShouldContainError() {

        val result =
            ExternalScheduleSourceResult.failure(
                sourceId =
                    sourceId,

                fetchedAt =
                    fetchedAt,

                error =
                    "读取失败",
            )

        assertTrue(
            !result.success
        )

        assertNull(
            result.schedule
        )

        assertEquals(
            "读取失败",
            result.error,
        )
    }

    @Test
    fun blankFailureErrorShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleSourceResult.failure(
                sourceId =
                    sourceId,

                fetchedAt =
                    fetchedAt,

                error =
                    " ",
            )
        }
    }
}
