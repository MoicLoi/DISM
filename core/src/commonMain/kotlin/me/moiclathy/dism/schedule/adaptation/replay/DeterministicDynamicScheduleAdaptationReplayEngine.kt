package me.moiclathy.dism.schedule.adaptation.replay

import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptationInput
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdapter

/**
 * 14.12
 *
 * 第一版确定性 Adaptation Replay Engine。
 *
 * 它复用 12.x 的 `DynamicSchedulePriorityAdapter`——
 * 也就是当初真正产生这份适配结果的那个算法。
 *
 * ### 刻意不走 14.x 的行为链
 *
 * 14.11 落盘的是 12.x 形状的适配结果
 * （`basePriority` / `adaptationFactor` / `effectivePriority`），
 * 所以重放也必须用产生它的那个算法。
 * 如果这里换成 14.4–14.6 的行为链，差异会永远非空，
 * 而且和"算法退化"完全无关。
 */
class DeterministicDynamicScheduleAdaptationReplayEngine(
    private val adapter: DynamicSchedulePriorityAdapter =
        me.moiclathy.dism.schedule.adaptation
            .DeterministicDynamicSchedulePriorityAdapter(),
) : DynamicScheduleAdaptationReplayEngine {

    override fun replay(
        input: DynamicScheduleAdaptationReplayInput,
        historical: List<DynamicSchedulePriorityAdaptation>,
    ): DynamicScheduleAdaptationReplayResult {

        val replayedResult =
            adapter.adapt(
                input =
                    DynamicSchedulePriorityAdaptationInput(
                        model = input.model,
                        histories = input.histories,
                    ),
                policy = input.policy,
            )

        val replayed =
            replayedResult.adaptations

        val differences =
            mutableListOf<
                DynamicScheduleAdaptationReplayDifference
            >()

        compare(
            historical = historical,
            replayed = replayed,
            differences = differences,
        )

        return DynamicScheduleAdaptationReplayResult(
            historical = historical,
            replayed = replayed,
            differences = differences,
            equivalent = differences.isEmpty(),
        )
    }

    private fun compare(
        historical:
            List<DynamicSchedulePriorityAdaptation>,

        replayed:
            List<DynamicSchedulePriorityAdaptation>,

        differences:
            MutableList<
                DynamicScheduleAdaptationReplayDifference
            >,
    ) {

        val historicalById =
            historical.associateBy {
                it.itemId
            }

        val replayedById =
            replayed.associateBy {
                it.itemId
            }

        /*
         * 必须排序。
         *
         * 规格原文这里只做了 `toSet()`，而 Set 的遍历顺序
         * 取决于哈希布局——同一个输入在不同进程里
         * 可能得到不同顺序的 differences。
         *
         * 对一个要做指纹、要写进 Provenance 的差异列表来说，
         * 顺序不确定等于不可复现。
         *
         * 注意用 `sortedBy` 而不是 `toSortedSet()`：
         * 后者只在 JVM 上可用，KMP 的 JS / Wasm 编译不过。
         */
        val allIds =
            (historicalById.keys + replayedById.keys)
                .sortedBy { it.value }

        allIds.forEach { itemId ->

            val old =
                historicalById[itemId]

            val current =
                replayedById[itemId]

            if (old == null && current != null) {

                differences +=
                    DynamicScheduleAdaptationReplayDifference(
                        itemId = itemId.toString(),
                        type =
                            DynamicScheduleAdaptationReplayDifferenceType
                                .ITEM_ADDED,
                        historicalValue = null,
                        replayedValue =
                            current.toString(),
                        message =
                            "Item exists only in replayed adaptation",
                    )

                return@forEach
            }

            if (old != null && current == null) {

                differences +=
                    DynamicScheduleAdaptationReplayDifference(
                        itemId = itemId.toString(),
                        type =
                            DynamicScheduleAdaptationReplayDifferenceType
                                .ITEM_MISSING,
                        historicalValue =
                            old.toString(),
                        replayedValue = null,
                        message =
                            "Item exists only in historical adaptation",
                    )

                return@forEach
            }

            check(old != null)
            check(current != null)

            compareDouble(
                itemId = itemId.toString(),
                type =
                    DynamicScheduleAdaptationReplayDifferenceType
                        .BASE_PRIORITY_CHANGED,
                historical = old.basePriority,
                replayed = current.basePriority,
                differences = differences,
                message = "Base priority changed",
            )

            compareDouble(
                itemId = itemId.toString(),
                type =
                    DynamicScheduleAdaptationReplayDifferenceType
                        .ADAPTATION_FACTOR_CHANGED,
                historical = old.adaptationFactor,
                replayed = current.adaptationFactor,
                differences = differences,
                message = "Adaptation factor changed",
            )

            compareDouble(
                itemId = itemId.toString(),
                type =
                    DynamicScheduleAdaptationReplayDifferenceType
                        .EFFECTIVE_PRIORITY_CHANGED,
                historical = old.effectivePriority,
                replayed = current.effectivePriority,
                differences = differences,
                message = "Effective priority changed",
            )

            if (old.reasons != current.reasons) {

                differences +=
                    DynamicScheduleAdaptationReplayDifference(
                        itemId = itemId.toString(),
                        type =
                            DynamicScheduleAdaptationReplayDifferenceType
                                .REASONS_CHANGED,
                        historicalValue =
                            old.reasons.joinToString(),
                        replayedValue =
                            current.reasons.joinToString(),
                        message =
                            "Adaptation reasons changed",
                    )
            }
        }
    }

    private fun compareDouble(
        itemId: String,
        type: DynamicScheduleAdaptationReplayDifferenceType,
        historical: Double,
        replayed: Double,
        differences:
            MutableList<
                DynamicScheduleAdaptationReplayDifference
            >,
        message: String,
    ) {

        if (historical != replayed) {

            differences +=
                DynamicScheduleAdaptationReplayDifference(
                    itemId = itemId,
                    type = type,
                    historicalValue =
                        historical.toString(),
                    replayedValue =
                        replayed.toString(),
                    message = message,
                )
        }
    }
}
