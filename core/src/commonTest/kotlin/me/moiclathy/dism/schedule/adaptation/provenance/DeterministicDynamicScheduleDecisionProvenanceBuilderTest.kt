package me.moiclathy.dism.schedule.adaptation.provenance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationBoundary
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.adaptation.provenance
    .DynamicScheduleDecisionProvenanceCompleteness
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.snapshot
    .DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTrace
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTraceEntry
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTraceStage

/**
 * 14.14 / 14.15
 *
 * Provenance 的核心价值是**可信**，所以这些测试的重点不是
 * "字段对不对"，而是：
 *
 * ```text
 * 没有的东西，绝不能出现在图里。
 * ```
 *
 * 尤其是 14.14 §8 警告过的那条：
 *
 * ```text
 * Provenance:  Human Override → xxx
 * 真实 Runtime: 根本没有这个 Override
 * ```
 *
 * 一旦发生，整张图就不可信了。
 */
class DeterministicDynamicScheduleDecisionProvenanceBuilderTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val generatedAt =
        Instant.parse("2026-01-01T00:00:01Z")

    private fun snapshot(): DynamicScheduleDecisionSnapshot =
        DynamicScheduleReplayTestData.decisionSnapshot()

    private fun trace(): DynamicScheduleDecisionTraceRecord =
        DynamicScheduleDecisionTraceRecord(
            trace =
                DynamicScheduleDecisionTrace(
                    itemId = itemId,
                    entries =
                        listOf(
                            DynamicScheduleDecisionTraceEntry(
                                stage =
                                    DynamicScheduleDecisionTraceStage
                                        .INPUT,
                                message =
                                    "Input received.",
                            ),
                            DynamicScheduleDecisionTraceEntry(
                                stage =
                                    DynamicScheduleDecisionTraceStage
                                        .PRIORITY_ADAPTATION,
                                message =
                                    "Priority adapted.",
                                decisive = true,
                            ),
                            DynamicScheduleDecisionTraceEntry(
                                stage =
                                    DynamicScheduleDecisionTraceStage
                                        .OUTPUT,
                                message =
                                    "Output produced.",
                                decisive = true,
                            ),
                        ),
                ),
            createdAt =
                Instant.parse("2026-01-01T00:00:00Z"),
            scheduleRevision = 1L,
            engineVersion = "test",
        )

    private fun adaptation(
        factor: Double = 1.2,
    ) =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.5,
            adaptationFactor = factor,
            effectivePriority = (0.5 * factor).coerceIn(0.0, 1.0),
            reasons = listOf("engagedCount=2"),
        )

    @Test
    fun provenanceContainsDecisionChain() {

        val snapshot = snapshot()

        val trace = trace()

        val provenance =
            DeterministicDynamicScheduleDecisionProvenanceBuilder()
                .build(
                    snapshot = snapshot,
                    trace = trace,
                    adaptations = listOf(adaptation()),
                    generatedAt = generatedAt,
                    engineVersion = "test",
                )

        assertEquals(itemId, provenance.itemId)

        assertEquals(snapshot.id, provenance.snapshotId)

        assertEquals(trace.traceId, provenance.traceId)

        assertTrue(provenance.complete)

        assertEquals(
            DynamicScheduleDecisionProvenanceCompleteness.COMPLETE,
            provenance.completeness,
        )

        /*
         * INPUT / SNAPSHOT / TRACE / DEPENDENCY / CONTEXT /
         * ATTENTION_MEMORY / PRIORITY_ADAPTATION / OUTPUT
         */
        assertEquals(8, provenance.nodeCount)

        assertTrue(provenance.decisiveNodes.isNotEmpty())

        assertTrue(
            provenance.contains(
                DynamicScheduleDecisionProvenanceNodeType
                    .PRIORITY_ADAPTATION,
            ),
        )

        assertTrue(
            provenance.contains(
                DynamicScheduleDecisionProvenanceNodeType.OUTPUT,
            ),
        )
    }

    @Test
    fun missingTraceMakesProvenancePartial() {

        val provenance =
            DeterministicDynamicScheduleDecisionProvenanceBuilder()
                .build(
                    snapshot = snapshot(),
                    trace = null,
                    adaptations = listOf(adaptation()),
                    generatedAt = generatedAt,
                    engineVersion = "test",
                )

        assertFalse(provenance.complete)

        assertEquals(
            DynamicScheduleDecisionProvenanceCompleteness.PARTIAL,
            provenance.completeness,
        )

        assertTrue(
            provenance.diagnostics.any {
                it.contains("trace")
            },
        )

        assertFalse(
            provenance.contains(
                DynamicScheduleDecisionProvenanceNodeType.TRACE,
            ),
            "a missing trace must not produce a TRACE node",
        )
    }

    @Test
    fun missingAdaptationMakesProvenancePartialButStillValid() {

        val provenance =
            DeterministicDynamicScheduleDecisionProvenanceBuilder()
                .build(
                    snapshot = snapshot(),
                    trace = trace(),
                    adaptations = emptyList(),
                    generatedAt = generatedAt,
                    engineVersion = "test",
                )

        assertEquals(
            DynamicScheduleDecisionProvenanceCompleteness.PARTIAL,
            provenance.completeness,
        )

        assertFalse(
            provenance.contains(
                DynamicScheduleDecisionProvenanceNodeType
                    .PRIORITY_ADAPTATION,
            ),
            "a missing adaptation must not produce a PRIORITY_ADAPTATION node",
        )

        assertTrue(
            provenance.diagnostics.any {
                it.contains("adaptation")
            },
        )
    }

    @Test
    fun nothingBeyondSnapshotIsEmpty() {

        val provenance =
            DeterministicDynamicScheduleDecisionProvenanceBuilder()
                .build(
                    snapshot = snapshot(),
                    trace = null,
                    adaptations = emptyList(),
                    generatedAt = generatedAt,
                    engineVersion = "test",
                )

        assertEquals(
            DynamicScheduleDecisionProvenanceCompleteness.EMPTY,
            provenance.completeness,
        )
    }

    @Test
    fun optionalNodesAppearOnlyWhenSupplied() {

        val snapshot = snapshot()

        /*
         * 只给 snapshot + trace + adaptation。
         */
        val minimal =
            DeterministicDynamicScheduleDecisionProvenanceBuilder()
                .build(
                    snapshot = snapshot,
                    trace = trace(),
                    adaptations = listOf(adaptation()),
                    generatedAt = generatedAt,
                    engineVersion = "test",
                )

        listOf(
            DynamicScheduleDecisionProvenanceNodeType
                .HUMAN_OVERRIDE,
            DynamicScheduleDecisionProvenanceNodeType
                .ADAPTATION_BOUNDARY,
            DynamicScheduleDecisionProvenanceNodeType.REPLAY,
            DynamicScheduleDecisionProvenanceNodeType
                .VERIFICATION,
        ).forEach { type ->
            assertFalse(
                minimal.contains(type),
                "$type must not be fabricated",
            )
        }

        /*
         * 现在把它们真的提供出来。
         */
        val full =
            DeterministicDynamicScheduleDecisionProvenanceBuilder()
                .build(
                    DynamicScheduleDecisionProvenanceInput(
                        snapshot = snapshot,
                        trace = trace(),
                        adaptations = listOf(adaptation()),
                        generatedAt = generatedAt,
                        engineVersion = "test",

                        boundary =
                            DynamicScheduleAdaptationBoundary(
                                itemId = itemId,
                                requestedDelta = 0.30,
                                allowedDelta = 0.10,
                                confidence = 0.9,
                                limitedBySingleChange = true,
                                limitedByCumulativeChange = false,
                                limitedByFrequency = false,
                                limitedByExplicitUserSetting = false,
                                enabled = true,
                            ),

                        humanOverride =
                            DynamicScheduleHumanOverride(
                                itemId = itemId,
                                priorityOverride = 0.9,
                                createdAt = generatedAt,
                                reason = "User explicitly marked important",
                            ),

                        replayed = true,

                        verificationStatus = "EXACT_MATCH",
                    ),
                )

        listOf(
            DynamicScheduleDecisionProvenanceNodeType
                .HUMAN_OVERRIDE,
            DynamicScheduleDecisionProvenanceNodeType
                .ADAPTATION_BOUNDARY,
            DynamicScheduleDecisionProvenanceNodeType.REPLAY,
            DynamicScheduleDecisionProvenanceNodeType
                .VERIFICATION,
        ).forEach { type ->
            assertTrue(
                full.contains(type),
                "$type should exist when it was actually supplied",
            )
        }

        /*
         * 用户覆盖必须出现在最终输出的祖先链上——
         * 否则"为什么这条是这个优先级"就答不出来。
         */
        val ancestors =
            full.ancestorsOf(full.outputNodeId)

        assertTrue(
            ancestors.any {
                it.type ==
                    DynamicScheduleDecisionProvenanceNodeType
                        .HUMAN_OVERRIDE
            },
            "the override must be reachable from the output",
        )
    }
}
