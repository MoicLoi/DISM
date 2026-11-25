package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleContextSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDependencySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicSchedulePolicySnapshot

class DeterministicDynamicScheduleDecisionReplayReportBuilderTest {

    private val builder =
        DeterministicDynamicScheduleDecisionReplayReportBuilder()

    private fun snapshot(): DynamicScheduleDecisionSnapshot {
        return DynamicScheduleDecisionSnapshot(
            itemId =
                DynamicScheduleItemId("item-1"),

            capturedAt =
                Instant.parse(
                    "2026-09-23T08:00:00Z",
                ),

            scheduleRevision = 10,

            engineVersion = "12.19",

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
                    requiredSatisfied = true,
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
    fun equivalentReplayProducesNoDifference() {

        val replay =
            DynamicScheduleSnapshotReplayResult(
                snapshot = snapshot(),

                originalPriority = 0.4,

                currentPriority = 0.4,

                originalAttentionRequired = true,

                currentAttentionRequired = true,

                priorityChanged = false,

                attentionDecisionChanged = false,

                conflictChanged = false,

                differences = emptyList(),
            )

        val report =
            builder.build(replay)

        assertEquals(
            DynamicScheduleDecisionReplaySeverity.NONE,
            report.severity,
        )

        assertTrue(
            report.equivalent,
        )

        assertTrue(
            report.changedFactors.isEmpty(),
        )
    }

    @Test
    fun changedPriorityProducesBehaviorChange() {

        val difference =
            DynamicScheduleSnapshotReplayDifference(
                type =
                    DynamicScheduleSnapshotReplayDifferenceType
                        .PRIORITY_CHANGED,

                originalValue = "0.4",

                currentValue = "0.2",

                message =
                    "Decision priority changed",
            )

        val replay =
            DynamicScheduleSnapshotReplayResult(
                snapshot = snapshot(),

                originalPriority = 0.4,

                currentPriority = 0.2,

                originalAttentionRequired = true,

                currentAttentionRequired = true,

                priorityChanged = true,

                attentionDecisionChanged = false,

                conflictChanged = false,

                differences =
                    listOf(difference),
            )

        val report =
            builder.build(replay)

        assertEquals(
            DynamicScheduleDecisionReplaySeverity
                .BEHAVIOR_CHANGED,
            report.severity,
        )

        assertEquals(
            1,
            report.changedFactors.size,
        )

        assertTrue(
            report.summary.isNotBlank(),
        )
    }

    @Test
    fun changedAttentionDecisionProducesSignificantChange() {

        val difference =
            DynamicScheduleSnapshotReplayDifference(
                type =
                    DynamicScheduleSnapshotReplayDifferenceType
                        .ATTENTION_DECISION_CHANGED,

                originalValue = "true",

                currentValue = "false",

                message =
                    "Attention decision changed",
            )

        val replay =
            DynamicScheduleSnapshotReplayResult(
                snapshot = snapshot(),

                originalPriority = 0.4,

                currentPriority = 0.1,

                originalAttentionRequired = true,

                currentAttentionRequired = false,

                priorityChanged = true,

                attentionDecisionChanged = true,

                conflictChanged = false,

                differences =
                    listOf(difference),
            )

        val report =
            builder.build(replay)

        assertEquals(
            DynamicScheduleDecisionReplaySeverity
                .SIGNIFICANT,
            report.severity,
        )

        assertTrue(
            report.behaviorChanged,
        )

        assertTrue(
            report.summary.contains(
                "no longer requires attention",
            ),
        )
    }

    @Test
    fun attentionAndConflictChangeProducesCriticalResult() {

        val differences =
            listOf(
                DynamicScheduleSnapshotReplayDifference(
                    type =
                        DynamicScheduleSnapshotReplayDifferenceType
                            .ATTENTION_DECISION_CHANGED,

                    originalValue = "true",

                    currentValue = "false",

                    message =
                        "Attention decision changed",
                ),

                DynamicScheduleSnapshotReplayDifference(
                    type =
                        DynamicScheduleSnapshotReplayDifferenceType
                            .CONFLICT_CHANGED,

                    originalValue = "true",

                    currentValue = "false",

                    message =
                        "Conflict changed",
                ),
            )

        val replay =
            DynamicScheduleSnapshotReplayResult(
                snapshot = snapshot(),

                originalPriority = 0.75,

                currentPriority = 0.1,

                originalAttentionRequired = true,

                currentAttentionRequired = false,

                priorityChanged = true,

                attentionDecisionChanged = true,

                conflictChanged = true,

                differences = differences,
            )

        val report =
            builder.build(replay)

        assertEquals(
            DynamicScheduleDecisionReplaySeverity
                .CRITICAL,
            report.severity,
        )

        assertEquals(
            2,
            report.changedFactors.size,
        )
    }
}
