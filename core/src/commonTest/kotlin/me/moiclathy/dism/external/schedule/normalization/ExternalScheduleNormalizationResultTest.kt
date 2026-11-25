package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalScheduleNormalizationResultTest {

    private val schedule =
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
                emptyList(),
        )

    @Test
    fun successShouldBeSuccessful() {

        val result =
            ExternalScheduleNormalizationResult.success(
                schedule =
                    schedule,
            )

        assertTrue(
            result.success
        )

        assertFalse(
            result.hasWarnings
        )
    }

    @Test
    fun warningsShouldNotMakeResultFail() {

        val result =
            ExternalScheduleNormalizationResult.success(
                schedule =
                    schedule,

                warnings =
                    listOf(
                        "Some fields were normalized."
                    ),
            )

        assertTrue(
            result.success
        )

        assertTrue(
            result.hasWarnings
        )
    }

    @Test
    fun failureShouldNotBeSuccessful() {

        val result =
            ExternalScheduleNormalizationResult.failure(
                error =
                    "Invalid schedule.",
            )

        assertFalse(
            result.success
        )
    }
}
