package me.moiclathy.dism.event.duration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

class DeterministicEventDurationEngineTest {

    private val engine =
        DeterministicEventDurationEngine()

    @Test
    fun fixedDurationShouldResolve() {

        val result =
            engine.resolve(
                EventDuration(
                    value =
                        90.minutes,
                ),
            )

        assertEquals(
            90.minutes,
            result.duration.value,
        )

        assertEquals(
            90.minutes,
            result.estimate?.minimum,
        )

        assertEquals(
            90.minutes,
            result.estimate?.expected,
        )

        assertEquals(
            90.minutes,
            result.estimate?.maximum,
        )
    }

    @Test
    fun estimatedDurationShouldResolve() {

        val estimate =
            EventDurationEstimate(
                minimum =
                    5.minutes,

                expected =
                    10.minutes,

                maximum =
                    15.minutes,
            )

        val result =
            engine.resolveEstimate(
                estimate =
                    estimate,

                type =
                    EventDurationType.ESTIMATED,
            )

        assertEquals(
            10.minutes,
            result.duration.value,
        )

        assertEquals(
            EventDurationType.ESTIMATED,
            result.duration.type,
        )

        assertEquals(
            estimate,
            result.estimate,
        )
    }

    @Test
    fun derivedDurationShouldResolveByDefault() {

        val estimate =
            EventDurationEstimate(
                minimum =
                    8.minutes,

                expected =
                    12.minutes,

                maximum =
                    18.minutes,
            )

        val result =
            engine.resolveEstimate(
                estimate =
                    estimate,

                type =
                    EventDurationType.DERIVED,
            )

        assertEquals(
            EventDurationType.DERIVED,
            result.duration.type,
        )

        assertEquals(
            12.minutes,
            result.duration.value,
        )
    }

    @Test
    fun estimatedDurationCanBeDisabled() {

        val engine =
            DeterministicEventDurationEngine(
                EventDurationPolicy(
                    allowEstimated = false,
                ),
            )

        val estimate =
            EventDurationEstimate(
                minimum =
                    5.minutes,

                expected =
                    10.minutes,

                maximum =
                    15.minutes,
            )

        assertFailsWith<IllegalArgumentException> {

            engine.resolveEstimate(
                estimate =
                    estimate,

                type =
                    EventDurationType.ESTIMATED,
            )
        }
    }

    @Test
    fun derivedDurationCanBeDisabled() {

        val engine =
            DeterministicEventDurationEngine(
                EventDurationPolicy(
                    allowDerived = false,
                ),
            )

        val estimate =
            EventDurationEstimate(
                minimum =
                    5.minutes,

                expected =
                    10.minutes,

                maximum =
                    15.minutes,
            )

        assertFailsWith<IllegalArgumentException> {

            engine.resolveEstimate(
                estimate =
                    estimate,

                type =
                    EventDurationType.DERIVED,
            )
        }
    }

    @Test
    fun zeroDurationCanBeDisabled() {

        val engine =
            DeterministicEventDurationEngine(
                EventDurationPolicy(
                    allowZeroDuration = false,
                ),
            )

        assertFailsWith<IllegalArgumentException> {

            engine.resolve(
                EventDuration.zero(),
            )
        }
    }

    @Test
    fun fixedDurationShouldProduceExactEstimate() {

        val result =
            engine.resolve(
                EventDuration(
                    value =
                        30.minutes,
                ),
            )

        assertEquals(
            result.estimate?.minimum,
            result.estimate?.expected,
        )

        assertEquals(
            result.estimate?.expected,
            result.estimate?.maximum,
        )
    }

    @Test
    fun durationEngineShouldBeDeterministic() {

        val duration =
            EventDuration(
                value =
                    20.minutes,

                type =
                    EventDurationType.FIXED,
            )

        val first =
            engine.resolve(duration)

        val second =
            engine.resolve(duration)

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun estimateEngineShouldBeDeterministic() {

        val estimate =
            EventDurationEstimate(
                minimum =
                    3.minutes,

                expected =
                    5.minutes,

                maximum =
                    8.minutes,
            )

        val first =
            engine.resolveEstimate(
                estimate
            )

        val second =
            engine.resolveEstimate(
                estimate
            )

        assertEquals(
            first,
            second,
        )
    }
}
