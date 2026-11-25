package me.moiclathy.dism.schedule.adaptation.provenance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTrace
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceEntry
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage

/**
 * 14.16
 *
 * 解释层是**只读**的。
 *
 * 这几条测试锁住的核心不是"文字对不对"，而是：
 *
 * ```text
 * 解释不改变 Provenance
 * 解释不重新执行 Runtime
 * 解释不假装自己很完整
 * ```
 */
class DeterministicDynamicScheduleDecisionProvenanceExplainerTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val generatedAt =
        Instant.parse("2026-01-01T00:00:01Z")

    private fun snapshot() =
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
                                        .CONTEXT_BINDING,
                                message =
                                    "Context bound.",
                                decisive = true,
                            ),
                            DynamicScheduleDecisionTraceEntry(
                                stage =
                                    DynamicScheduleDecisionTraceStage
                                        .OUTPUT,
                                message = "Output produced.",
                            ),
                        ),
                ),
            createdAt = generatedAt,
            scheduleRevision = 1L,
            engineVersion = "test",
        )

    private fun adaptation() =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.5,
            adaptationFactor = 1.2,
            effectivePriority = 0.6,
            reasons = listOf("engagedCount=2"),
        )

    private fun provenance(
        withTrace: Boolean = true,
        withAdaptation: Boolean = true,
    ) =
        DeterministicDynamicScheduleDecisionProvenanceBuilder()
            .build(
                snapshot = snapshot(),
                trace = if (withTrace) trace() else null,
                adaptations =
                    if (withAdaptation) {
                        listOf(adaptation())
                    } else {
                        emptyList()
                    },
                generatedAt = generatedAt,
                engineVersion = "test",
            )

    @Test
    fun explanationDoesNotMutateProvenance() {

        val provenance = provenance()

        val before = provenance

        val explainer =
            DeterministicDynamicScheduleDecisionProvenanceExplainer()

        val explanation = explainer.explain(provenance)

        /*
         * 解释只是导出，
         * Provenance 本身必须逐字段不变。
         */
        assertEquals(before, provenance)

        assertEquals(
            before.nodes.map { it.id },
            provenance.nodes.map { it.id },
        )

        assertEquals(
            before.completeness,
            provenance.completeness,
        )

        /*
         * 解释也不应该反过来引用 Provenance 的可变状态——
         * 它拿到的是拷贝出来的 factors。
         */
        assertEquals(
            before.diagnostics,
            explanation.diagnostics,
        )
    }

    @Test
    fun explanationIsDeterministic() {

        val provenance = provenance()

        val explainer =
            DeterministicDynamicScheduleDecisionProvenanceExplainer()

        assertEquals(
            explainer.explain(provenance),
            explainer.explain(provenance),
        )
    }

    @Test
    fun explanationExcludesInputAndOutputNodes() {

        val explanation =
            DeterministicDynamicScheduleDecisionProvenanceExplainer()
                .explain(provenance())

        assertFalse(
            explanation.factors.any {
                it.type ==
                    DynamicScheduleDecisionProvenanceNodeType.INPUT
            },
            "INPUT describes the pipeline ends, not this decision",
        )

        assertFalse(
            explanation.factors.any {
                it.type ==
                    DynamicScheduleDecisionProvenanceNodeType.OUTPUT
            },
        )

        assertTrue(explanation.factorCount > 0)

        assertTrue(
            explanation.decisiveFactors.isNotEmpty(),
        )

        assertTrue(
            explanation.decisiveFactors.all { it.decisive },
        )
    }

    @Test
    fun incompleteProvenanceSaysSoInTheSummary() {

        val complete =
            DeterministicDynamicScheduleDecisionProvenanceExplainer()
                .explain(provenance())

        assertTrue(complete.complete)

        assertFalse(
            complete.summary.contains("部分完整"),
            "a complete provenance must not apologize for itself",
        )

        val partial =
            DeterministicDynamicScheduleDecisionProvenanceExplainer()
                .explain(
                    provenance(withAdaptation = false),
                )

        assertFalse(partial.complete)

        assertTrue(
            partial.summary.contains("部分完整"),
            "an incomplete provenance must not sound confident; " +
                "summary=${partial.summary}",
        )
    }

    @Test
    fun queryRequiresAnAnchor() {

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleDecisionProvenanceQuery()
        }
    }

    @Test
    fun decisiveProjectionIsAStrictSubset() {

        val provenance = provenance()

        val all = provenance.nodes

        val decisive = provenance.decisiveNodes

        assertTrue(
            decisive.size < all.size,
            "the fixture should contain non-decisive nodes",
        )

        assertTrue(
            decisive.all { it.decisive },
        )

        /*
         * 投影必须保持原顺序——否则同一次查询的节点顺序
         * 会随"要 decisive 还是全部"而变化，无法复现。
         */
        assertEquals(
            all.filter { it.decisive },
            decisive,
        )
    }

    @Test
    fun queryResultCarriesTheProjectionSeparately() {

        /*
         * 结构化查询的核心：provenance 始终是完整图，
         * nodes 才是这次要的投影。
         */
        val provenance = provenance()

        val result =
            DynamicScheduleDecisionProvenanceQueryResult(
                provenance = provenance,
                nodes = provenance.decisiveNodes,
            )

        assertTrue(result.found)

        assertEquals(
            provenance.nodeCount,
            result.provenance?.nodeCount,
        )

        assertTrue(
            result.nodes.size < provenance.nodeCount,
        )
    }
}
