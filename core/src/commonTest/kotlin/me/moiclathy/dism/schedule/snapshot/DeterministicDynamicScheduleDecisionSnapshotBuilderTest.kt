package me.moiclathy.dism.schedule.snapshot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicScheduleDecisionSnapshotBuilderTest {

    private val builder =
        DeterministicDynamicScheduleDecisionSnapshotBuilder()

    @Test
    fun buildsSnapshot() {

        val itemId =
            DynamicScheduleItemId("item-1")

        val snapshot =
            builder.build(

                item =
                    DynamicScheduleItemSnapshot(
                        itemId = itemId,
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
                        globalFactor = 0.4,
                        contextFactor = 0.8,
                        adaptationFactor = 0.9,
                    ),

                dependency =
                    DynamicScheduleDependencySnapshot(
                        requiredSatisfied = false,
                        missingDependencyIds =
                            listOf("campus-card"),
                        requiredDependencyCount = 1,
                    ),

                policy =
                    DynamicSchedulePolicySnapshot(
                        minimumAttentionPriority = 0.25,
                        minimumAttentionConfidence = 0.25,
                        minimumHardConstraintPriority = 0.75,
                        explicitRequestPriority = 1.0,
                        maximumLearnedSuppression = 0.5,
                    ),

                capturedAt =
                    Instant.parse(
                        "2026-09-22T08:30:00Z",
                    ),

                scheduleRevision = 17,

                engineVersion = "12.17",
            )

        assertEquals(
            itemId,
            snapshot.itemId,
        )

        assertEquals(
            17,
            snapshot.scheduleRevision,
        )

        assertEquals(
            "12.17",
            snapshot.engineVersion,
        )

        assertEquals(
            0.4,
            snapshot.attentionMemory.globalFactor,
        )

        assertEquals(
            listOf("campus-card"),
            snapshot.dependency
                .missingDependencyIds,
        )

        assertTrue(
            snapshot.context
                .activeContextKeys
                .contains("CLASSROOM"),
        )
    }

    @Test
    fun effectiveMemoryFactorIsStable() {

        val memory =
            DynamicScheduleAttentionMemorySnapshot(
                globalFactor = 0.4,
                contextFactor = 0.5,
                adaptationFactor = 0.8,
            )

        /*
         * 浮点乘法不做精确相等比较：
         * 0.4 * 0.5 * 0.8 == 0.16000000000000003
         */
        assertEquals(
            0.16,
            memory.effectiveFactor,
            1e-12,
        )
    }
}
