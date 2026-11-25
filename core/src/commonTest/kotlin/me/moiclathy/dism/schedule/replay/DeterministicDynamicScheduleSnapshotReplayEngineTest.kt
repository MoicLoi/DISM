package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleContextSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDependencySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicSchedulePolicySnapshot
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicScheduleSnapshotReplayEngineTest {

    private val engine =
        DeterministicDynamicScheduleSnapshotReplayEngine()

    private fun snapshot(
        dependencySatisfied: Boolean = true,
    ): DynamicScheduleDecisionSnapshot {

        return DynamicScheduleDecisionSnapshot(

            itemId =
                DynamicScheduleItemId("item-1"),

            capturedAt =
                Instant.parse(
                    "2026-09-23T08:00:00Z",
                ),

            scheduleRevision = 10,

            engineVersion = "12.18",

            item =
                DynamicScheduleItemSnapshot(
                    itemId =
                        DynamicScheduleItemId("item-1"),

                    title = "实验课",

                    state =
                        DynamicScheduleState.READY,

                    priority = 0.8,

                    confidence = 1.0,
                ),

            context =
                DynamicScheduleContextSnapshot(
                    activeContextKeys =
                        setOf("CLASSROOM"),
                ),

            attentionMemory =
                DynamicScheduleAttentionMemorySnapshot(
                    globalFactor = 0.5,
                    contextFactor = 1.0,
                    adaptationFactor = 1.0,
                ),

            dependency =
                DynamicScheduleDependencySnapshot(
                    requiredSatisfied =
                        dependencySatisfied,
                ),

            policy =
                DynamicSchedulePolicySnapshot(
                    minimumAttentionPriority = 0.25,
                    minimumAttentionConfidence = 0.25,
                    minimumHardConstraintPriority = 0.75,
                    explicitRequestPriority = 1.0,
                    maximumLearnedSuppression = 0.5,
                ),
        )
    }

    @Test
    fun identicalDecisionIsEquivalent() {

        val snapshot =
            snapshot(
                dependencySatisfied = true,
            )

        val result =
            engine.replay(
                DynamicScheduleSnapshotReplayInput(
                    snapshot = snapshot,

                    currentPriority = 0.4,

                    currentAttentionRequired = true,
                ),
            )

        assertTrue(
            result.equivalent,
        )

        assertEquals(
            0.4,
            result.originalPriority,
        )
    }

    @Test
    fun changedPriorityIsDetected() {

        val result =
            engine.replay(
                DynamicScheduleSnapshotReplayInput(
                    snapshot =
                        snapshot(),

                    currentPriority = 0.1,

                    currentAttentionRequired = true,
                ),
            )

        assertTrue(
            result.priorityChanged,
        )
    }

    @Test
    fun hardDependencyProducesAttentionRequirement() {

        val result =
            engine.replay(
                DynamicScheduleSnapshotReplayInput(
                    snapshot =
                        snapshot(
                            dependencySatisfied = false,
                        ),

                    currentPriority = 0.2,

                    currentAttentionRequired = false,
                ),
            )

        assertEquals(
            0.75,
            result.originalPriority,
        )

        assertTrue(
            result.originalAttentionRequired,
        )

        assertTrue(
            result.attentionDecisionChanged,
        )
    }
}
