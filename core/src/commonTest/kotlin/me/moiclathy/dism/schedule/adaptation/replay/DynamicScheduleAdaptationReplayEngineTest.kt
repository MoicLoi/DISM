package me.moiclathy.dism.schedule.adaptation.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.adaptation
    .DeterministicDynamicSchedulePriorityAdapter
import me.moiclathy.dism.schedule.adaptation
    .DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptationInput
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData

/**
 * 14.12
 *
 * Adaptation Replay 的核心语义：
 *
 * ```text
 * 同样的输入 → 同样的结果 → 没有差异
 * ```
 *
 * 以及：
 *
 * ```text
 * policy 变了 → 差异必须被报出来
 * ```
 *
 * 第二点才是 14.12 真正存在的理由。没有它的话，
 * "算法退化"和"参数被改过"永远分不清。
 */
class DynamicScheduleAdaptationReplayEngineTest {

    private val engine =
        DeterministicDynamicScheduleAdaptationReplayEngine()

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private fun model() =
        DynamicScheduleReplayTestData.model()

    private fun histories(
        ignoredCount: Int = 2,
        engagedCount: Int = 3,
    ) = mapOf(
        itemId.value to
            DynamicScheduleAttentionHistory(
                attentionKey = itemId.value,
                shownCount = 10,
                engagedCount = engagedCount,
                ignoredCount = ignoredCount,
            ),
    )

    private fun input(
        policy: DynamicSchedulePriorityAdaptationPolicy =
            DynamicSchedulePriorityAdaptationPolicy(),
    ) =
        DynamicScheduleAdaptationReplayInput(
            model = model(),
            histories = histories(),
            policy = policy,
        )

    /**
     * 用给定输入真正跑一次适配，得到"历史结果"。
     */
    private fun historicalFor(
        input: DynamicScheduleAdaptationReplayInput,
    ): List<DynamicSchedulePriorityAdaptation> =
        DeterministicDynamicSchedulePriorityAdapter()
            .adapt(
                input =
                    DynamicSchedulePriorityAdaptationInput(
                        model = input.model,
                        histories = input.histories,
                    ),
                policy = input.policy,
            )
            .adaptations

    @Test
    fun identicalInputProducesNoDifferences() {

        val input = input()

        val result =
            engine.replay(
                input = input,
                historical = historicalFor(input),
            )

        assertTrue(
            result.equivalent,
            "replaying the same input must be a no-op, " +
                "differences=${result.differences}",
        )

        assertTrue(result.differences.isEmpty())

        assertFalse(result.changed)
    }

    @Test
    fun changedPolicyIsReportedAsDifference() {

        /*
         * 历史结果由"旧 policy"产生。
         */
        val historicalInput =
            input(
                policy =
                    DynamicSchedulePriorityAdaptationPolicy(
                        engagedBonus = 0.05,
                    ),
            )

        val historical =
            historicalFor(historicalInput)

        /*
         * 重放时 policy 已经被改过——但输入模型和历史都是同一份。
         */
        val replayedInput =
            input(
                policy =
                    DynamicSchedulePriorityAdaptationPolicy(
                        engagedBonus = 0.40,
                    ),
            )

        val result =
            engine.replay(
                input = replayedInput,
                historical = historical,
            )

        assertTrue(
            result.changed,
            "a changed policy must show up as a difference, " +
                "otherwise a policy edit looks like algorithm drift",
        )

        assertTrue(
            result.differences.any {
                it.type ==
                    DynamicScheduleAdaptationReplayDifferenceType
                        .ADAPTATION_FACTOR_CHANGED
            },
            "expected an ADAPTATION_FACTOR_CHANGED difference, " +
                "got=${result.differences.map { it.type }}",
        )
    }

    @Test
    fun missingHistoricalItemIsReported() {

        val input = input()

        val historical =
            historicalFor(input) +
                DynamicSchedulePriorityAdaptation(
                    itemId =
                        DynamicScheduleItemId("ghost-item"),
                    basePriority = 0.5,
                    adaptationFactor = 1.2,
                    effectivePriority = 0.6,
                )

        val result =
            engine.replay(
                input = input,
                historical = historical,
            )

        val difference =
            result.differences.single {
                it.type ==
                    DynamicScheduleAdaptationReplayDifferenceType
                        .ITEM_MISSING
            }

        assertEquals(
            "ghost-item",
            difference.itemId,
        )

        assertTrue(
            difference.replayedValue == null,
        )
    }

    @Test
    fun differenceOrderIsDeterministic() {

        /*
         * historical 里放三个 item，replayed 是空的，
         * 于是会产生三条 ITEM_MISSING。
         *
         * 如果顺序依赖 HashMap 布局，这个断言会不稳定。
         */
        val historical =
            listOf(
                "item-c",
                "item-a",
                "item-b",
            ).map { id ->
                DynamicSchedulePriorityAdaptation(
                    itemId = DynamicScheduleItemId(id),
                    basePriority = 0.5,
                    adaptationFactor = 1.1,
                    effectivePriority = 0.55,
                )
            }

        val result =
            engine.replay(
                input = input(),
                historical = historical,
            )

        /*
         * 完整列表按 itemId 字典序：
         * "item-1"（model 里真实存在，只在 replayed 侧）排在
         * "item-a" 之前，因为 '1' < 'a'。
         */
        assertEquals(
            listOf("item-1", "item-a", "item-b", "item-c"),
            result.differences.map { it.itemId },
        )

        /*
         * 真正要证明的是 ITEM_MISSING 的顺序稳定。
         */
        assertEquals(
            listOf("item-a", "item-b", "item-c"),
            result.differences
                .filter {
                    it.type ==
                        DynamicScheduleAdaptationReplayDifferenceType
                            .ITEM_MISSING
                }
                .map { it.itemId },
        )
    }
}
