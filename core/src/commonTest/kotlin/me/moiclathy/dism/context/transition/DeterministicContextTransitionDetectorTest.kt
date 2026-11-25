package me.moiclathy.dism.context.transition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

class DeterministicContextTransitionDetectorTest {

    private val detector =
        DeterministicContextTransitionDetector()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val policy =
        ContextTransitionPolicy()

    @Test
    fun nullToContextProducesEntered() {

        val result =
            detector.detect(
                previousMode = null,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.95,
                now = now,
                policy = policy,
            )

        assertTrue(result.applied)
        assertTrue(result.changed)

        assertEquals(
            ContextTransitionType.ENTERED,
            result.transition?.type,
        )

        assertNull(
            result.transition?.from
        )

        assertEquals(
            ContextMode.IN_CLASS,
            result.transition?.to,
        )
    }

    @Test
    fun contextToDifferentContextProducesChanged() {

        val result =
            detector.detect(
                previousMode = ContextMode.IN_TRANSIT,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.95,
                now = now,
                policy = policy,
            )

        assertTrue(result.applied)

        assertEquals(
            ContextTransitionType.CHANGED,
            result.transition?.type,
        )

        assertEquals(
            ContextMode.IN_TRANSIT,
            result.transition?.from,
        )

        assertEquals(
            ContextMode.IN_CLASS,
            result.transition?.to,
        )
    }

    @Test
    fun contextToNullProducesExited() {

        val result =
            detector.detect(
                previousMode = ContextMode.IN_CLASS,
                currentMode = null,
                currentConfidence = 1.0,
                now = now,
                policy = policy,
            )

        assertTrue(result.applied)

        assertEquals(
            ContextTransitionType.EXITED,
            result.transition?.type,
        )

        assertEquals(
            ContextMode.IN_CLASS,
            result.transition?.from,
        )

        assertNull(
            result.transition?.to
        )
    }

    @Test
    fun sameContextProducesNothingByDefault() {

        val result =
            detector.detect(
                previousMode = ContextMode.IN_CLASS,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.95,
                now = now,
                policy = policy,
            )

        assertFalse(result.applied)
        assertFalse(result.changed)

        assertNull(
            result.transition
        )
    }

    @Test
    fun sameContextCanProducePersisting() {

        val result =
            detector.detect(
                previousMode = ContextMode.IN_CLASS,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.95,
                now = now,
                policy = policy.copy(
                    emitPersisting = true,
                ),
            )

        assertFalse(result.applied)
        assertTrue(result.persisting)

        assertEquals(
            ContextTransitionType.PERSISTING,
            result.transition?.type,
        )
    }

    @Test
    fun lowConfidenceDoesNotTriggerTransition() {

        val result =
            detector.detect(
                previousMode = ContextMode.NORMAL,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.20,
                now = now,
                policy = policy,
            )

        assertFalse(result.applied)
        assertNull(result.transition)

        /*
         * 关键：
         *
         * 当前 Context 不应该因为低置信度
         * 被偷偷改掉。
         */
        assertEquals(
            ContextMode.NORMAL,
            result.currentMode,
        )
    }

    @Test
    fun lowConfidenceDoesNotTriggerExit() {

        val result =
            detector.detect(
                previousMode = ContextMode.IN_CLASS,
                currentMode = null,
                currentConfidence = 0.20,
                now = now,
                policy = policy,
            )

        /*
         * currentMode == null 时，
         * 这里不会因为 confidence
         * 自动否定旧 Context。
         *
         * 是否退出由 allowExitToUnknown 控制。
         */
        assertTrue(result.applied)
        assertEquals(
            ContextTransitionType.EXITED,
            result.transition?.type,
        )
    }

    @Test
    fun exitCanBeDisabled() {

        val result =
            detector.detect(
                previousMode = ContextMode.IN_CLASS,
                currentMode = null,
                currentConfidence = 1.0,
                now = now,
                policy = policy.copy(
                    allowExitToUnknown = false,
                ),
            )

        assertFalse(result.applied)
        assertNull(result.transition)

        assertEquals(
            ContextMode.IN_CLASS,
            result.currentMode,
        )
    }

    @Test
    fun bothNullProducesNoTransition() {

        val result =
            detector.detect(
                previousMode = null,
                currentMode = null,
                currentConfidence = 0.0,
                now = now,
                policy = policy,
            )

        assertFalse(result.applied)
        assertNull(result.transition)
        assertNull(result.currentMode)
    }

    @Test
    fun transitionIsDeterministic() {

        val first =
            detector.detect(
                previousMode = ContextMode.IN_TRANSIT,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.92,
                now = now,
                policy = policy,
            )

        val second =
            detector.detect(
                previousMode = ContextMode.IN_TRANSIT,
                currentMode = ContextMode.IN_CLASS,
                currentConfidence = 0.92,
                now = now,
                policy = policy,
            )

        assertEquals(
            first,
            second,
        )
    }
}
