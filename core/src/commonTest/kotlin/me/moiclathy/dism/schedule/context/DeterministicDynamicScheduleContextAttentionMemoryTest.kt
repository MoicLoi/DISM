package me.moiclathy.dism.schedule.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleContextAttentionMemoryTest {

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val now =
        Instant.parse(
            "2026-09-21T10:00:00Z",
        )

    @Test
    fun noContextHistoryKeepsNeutralFactor() {

        val memory =
            DeterministicDynamicScheduleContextAttentionMemory()

        val result =
            memory.evaluate(
                DynamicScheduleContextAttentionInput(
                    itemId = itemId,
                    activeContextKeys =
                        setOf("DORM"),
                    histories = emptyMap(),
                    now = now,
                ),
            )

        assertEquals(
            1.0,
            result.effectiveFactor,
        )
    }

    @Test
    fun ignoredHistoryReducesContextAttention() {

        val key =
            DynamicScheduleContextAttentionKey(
                itemId = itemId,
                contextKey = "DORM",
            )

        val history =
            DynamicScheduleContextAttentionHistory(
                key = key,
                shownCount = 10,
                ignoredCount = 5,
                lastInteractionAt = now,
            )

        val memory =
            DeterministicDynamicScheduleContextAttentionMemory()

        val result =
            memory.evaluate(
                DynamicScheduleContextAttentionInput(
                    itemId = itemId,
                    activeContextKeys =
                        setOf("DORM"),
                    histories =
                        mapOf(key to history),
                    now = now,
                ),
            )

        assertTrue(
            result.effectiveFactor < 1.0,
        )
    }

    @Test
    fun requestedHistoryRaisesContextAttention() {

        val key =
            DynamicScheduleContextAttentionKey(
                itemId = itemId,
                contextKey = "CLASSROOM",
            )

        val history =
            DynamicScheduleContextAttentionHistory(
                key = key,
                shownCount = 5,
                requestedCount = 2,
                lastInteractionAt = now,
            )

        val memory =
            DeterministicDynamicScheduleContextAttentionMemory()

        val result =
            memory.evaluate(
                DynamicScheduleContextAttentionInput(
                    itemId = itemId,
                    activeContextKeys =
                        setOf("CLASSROOM"),
                    histories =
                        mapOf(key to history),
                    now = now,
                ),
            )

        assertTrue(
            result.effectiveFactor > 1.0,
        )
    }

    @Test
    fun inactiveContextDoesNotAffectCurrentAttention() {

        val key =
            DynamicScheduleContextAttentionKey(
                itemId = itemId,
                contextKey = "CLASSROOM",
            )

        val history =
            DynamicScheduleContextAttentionHistory(
                key = key,
                shownCount = 5,
                requestedCount = 5,
                lastInteractionAt = now,
            )

        val memory =
            DeterministicDynamicScheduleContextAttentionMemory()

        val result =
            memory.evaluate(
                DynamicScheduleContextAttentionInput(
                    itemId = itemId,
                    activeContextKeys =
                        setOf("DORM"),
                    histories =
                        mapOf(key to history),
                    now = now,
                ),
            )

        assertEquals(
            1.0,
            result.effectiveFactor,
        )
    }

    @Test
    fun strongestContextSignalWins() {

        val dormKey =
            DynamicScheduleContextAttentionKey(
                itemId = itemId,
                contextKey = "DORM",
            )

        val classroomKey =
            DynamicScheduleContextAttentionKey(
                itemId = itemId,
                contextKey = "CLASSROOM",
            )

        val histories =
            mapOf(
                dormKey to
                    DynamicScheduleContextAttentionHistory(
                        key = dormKey,
                        ignoredCount = 5,
                        lastInteractionAt = now,
                    ),

                classroomKey to
                    DynamicScheduleContextAttentionHistory(
                        key = classroomKey,
                        requestedCount = 2,
                        lastInteractionAt = now,
                    ),
            )

        val memory =
            DeterministicDynamicScheduleContextAttentionMemory()

        val result =
            memory.evaluate(
                DynamicScheduleContextAttentionInput(
                    itemId = itemId,
                    activeContextKeys =
                        setOf(
                            "DORM",
                            "CLASSROOM",
                        ),
                    histories = histories,
                    now = now,
                ),
            )

        assertTrue(
            result.effectiveFactor > 1.0,
        )
    }
}
