package me.moiclathy.dism.event.alarm

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class DeterministicDynamicAlarmEngineTest {

    private val engine =
        DeterministicDynamicAlarmEngine()

    private val evaluatedAt =
        Instant.parse(
            "2026-09-16T09:00:00Z"
        )

    private val startAt =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    private val deadlineAt =
        Instant.parse(
            "2026-09-16T11:30:00Z"
        )

    @Test
    fun beforeStartShouldGenerateAlarm() {

        val result =
            engine.beforeStart(
                startAt =
                    startAt,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            result.generated
        )

        assertEquals(
            DynamicAlarmType.BEFORE_START,
            result.alarm!!.type,
        )

        assertEquals(
            Instant.parse(
                "2026-09-16T09:45:00Z"
            ),
            result.alarm!!.triggerAt,
        )
    }

    @Test
    fun customLeadTimeShouldBeUsed() {

        val result =
            engine.beforeStart(
                startAt =
                    startAt,

                evaluatedAt =
                    evaluatedAt,

                leadTime =
                    Duration.parse("30m"),
            )

        assertTrue(
            result.generated
        )

        assertEquals(
            Instant.parse(
                "2026-09-16T09:30:00Z"
            ),
            result.alarm!!.triggerAt,
        )

        assertEquals(
            Duration.parse("30m"),
            result.alarm!!.leadTime,
        )
    }

    @Test
    fun missingStartShouldNotGenerateAlarm() {

        val result =
            engine.beforeStart(
                startAt =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            !result.generated
        )

        assertNull(
            result.alarm
        )
    }

    @Test
    fun alreadyReachedTriggerShouldNotGenerateFutureAlarm() {

        val result =
            engine.beforeStart(
                startAt =
                    startAt,

                evaluatedAt =
                    Instant.parse(
                        "2026-09-16T09:50:00Z"
                    ),
            )

        assertTrue(
            !result.generated
        )

        assertNull(
            result.alarm
        )
    }

    @Test
    fun beforeDeadlineShouldGenerateAlarm() {

        val result =
            engine.beforeDeadline(
                deadlineAt =
                    deadlineAt,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            result.generated
        )

        assertEquals(
            DynamicAlarmType.BEFORE_DEADLINE,
            result.alarm!!.type,
        )

        assertEquals(
            Instant.parse(
                "2026-09-16T11:15:00Z"
            ),
            result.alarm!!.triggerAt,
        )
    }

    @Test
    fun missingDeadlineShouldNotGenerateAlarm() {

        val result =
            engine.beforeDeadline(
                deadlineAt =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            !result.generated
        )

        assertNull(
            result.alarm
        )
    }

    @Test
    fun criticalSlackShouldGenerateAlarm() {

        val actual =
            Instant.parse(
                "2026-09-16T11:25:00Z"
            )

        val result =
            engine.evaluateSlack(
                boundary =
                    deadlineAt,

                actual =
                    actual,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            result.generated
        )

        assertEquals(
            DynamicAlarmType.SLACK_CRITICAL,
            result.alarm!!.type,
        )

        assertEquals(
            evaluatedAt,
            result.alarm!!.triggerAt,
        )
    }

    @Test
    fun exhaustedSlackShouldGenerateAlarm() {

        val actual =
            Instant.parse(
                "2026-09-16T11:35:00Z"
            )

        val result =
            engine.evaluateSlack(
                boundary =
                    deadlineAt,

                actual =
                    actual,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            result.generated
        )

        assertEquals(
            DynamicAlarmType.SLACK_EXHAUSTED,
            result.alarm!!.type,
        )

        assertEquals(
            evaluatedAt,
            result.alarm!!.triggerAt,
        )
    }

    @Test
    fun sufficientSlackShouldNotGenerateAlarm() {

        val actual =
            Instant.parse(
                "2026-09-16T10:00:00Z"
            )

        val result =
            engine.evaluateSlack(
                boundary =
                    deadlineAt,

                actual =
                    actual,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            !result.generated
        )

        assertNull(
            result.alarm
        )
    }

    @Test
    fun missingSlackInputShouldNotGenerateAlarm() {

        val result =
            engine.evaluateSlack(
                boundary =
                    null,

                actual =
                    evaluatedAt,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            !result.generated
        )

        assertNull(
            result.alarm
        )
    }

    @Test
    fun disabledCriticalAlarmShouldNotGenerate() {

        val customEngine =
            DeterministicDynamicAlarmEngine(
                policy =
                    DynamicAlarmPolicy(
                        allowCriticalSlackAlarm =
                            false,
                    )
            )

        val result =
            customEngine.evaluateSlack(
                boundary =
                    deadlineAt,

                actual =
                    Instant.parse(
                        "2026-09-16T11:25:00Z"
                    ),

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            !result.generated
        )
    }

    @Test
    fun disabledExhaustedAlarmShouldNotGenerate() {

        val customEngine =
            DeterministicDynamicAlarmEngine(
                policy =
                    DynamicAlarmPolicy(
                        allowSlackExhaustedAlarm =
                            false,
                    )
            )

        val result =
            customEngine.evaluateSlack(
                boundary =
                    deadlineAt,

                actual =
                    Instant.parse(
                        "2026-09-16T11:35:00Z"
                    ),

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            !result.generated
        )
    }

    @Test
    fun engineShouldBeDeterministic() {

        val first =
            engine.beforeStart(
                startAt =
                    startAt,

                evaluatedAt =
                    evaluatedAt,
            )

        val second =
            engine.beforeStart(
                startAt =
                    startAt,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            first,
            second,
        )
    }
}
