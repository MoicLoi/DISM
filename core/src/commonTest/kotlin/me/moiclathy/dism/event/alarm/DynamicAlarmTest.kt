package me.moiclathy.dism.event.alarm

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration

class DynamicAlarmTest {

    private val triggerAt =
        Instant.parse(
            "2026-09-16T09:45:00Z"
        )

    @Test
    fun alarmShouldStoreTriggerTime() {

        val alarm =
            DynamicAlarm(
                type =
                    DynamicAlarmType.BEFORE_START,

                triggerAt =
                    triggerAt,

                reason =
                    "Test alarm.",
            )

        assertEquals(
            triggerAt,
            alarm.triggerAt,
        )
    }

    @Test
    fun alarmShouldRejectBlankReason() {

        assertFailsWith<IllegalArgumentException> {

            DynamicAlarm(
                type =
                    DynamicAlarmType.BEFORE_START,

                triggerAt =
                    triggerAt,

                reason =
                    " ",
            )
        }
    }

    @Test
    fun alarmShouldRejectNegativeLeadTime() {

        assertFailsWith<IllegalArgumentException> {

            DynamicAlarm(
                type =
                    DynamicAlarmType.BEFORE_START,

                triggerAt =
                    triggerAt,

                reason =
                    "Test alarm.",

                leadTime =
                    Duration.parse("-1m"),
            )
        }
    }

    @Test
    fun generatedEvaluationShouldContainAlarm() {

        val evaluation =
            DynamicAlarmEvaluation(
                alarm =
                    DynamicAlarm(
                        type =
                            DynamicAlarmType.BEFORE_START,

                        triggerAt =
                            triggerAt,

                        reason =
                            "Test alarm.",
                    ),

                evaluatedAt =
                    triggerAt,

                generated =
                    true,
            )

        assertTrue(
            evaluation.generated
        )

        assertTrue(
            evaluation.alarm != null
        )
    }

    @Test
    fun nonGeneratedEvaluationShouldNotContainAlarm() {

        val evaluation =
            DynamicAlarmEvaluation(
                alarm = null,

                evaluatedAt =
                    triggerAt,

                generated =
                    false,

                reason =
                    "No alarm required.",
            )

        assertTrue(
            !evaluation.generated
        )

        assertEquals(
            null,
            evaluation.alarm,
        )
    }

    @Test
    fun policyShouldRejectNegativeLeadTime() {

        assertFailsWith<IllegalArgumentException> {

            DynamicAlarmPolicy(
                defaultLeadTime =
                    Duration.parse("-1m")
            )
        }
    }
}
