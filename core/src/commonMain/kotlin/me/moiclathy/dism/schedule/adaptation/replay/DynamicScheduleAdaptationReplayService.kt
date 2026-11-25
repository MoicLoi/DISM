package me.moiclathy.dism.schedule.adaptation.replay

import me.moiclathy.dism.schedule.adaptation.persistence
    .DynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.12
 *
 * 按 item 重放一次历史适配。
 *
 * ```text
 * 14.11 的历史结果  +
 * 14.12 的历史输入  →  差异
 * ```
 *
 * ### 缺任意一半都直接报错，而不是返回"空差异"
 *
 * 返回"没有差异"是最坏的选择：调用方会以为算法是稳定的，
 * 而实际上是数据不完整。
 */
class DynamicScheduleAdaptationReplayService(
    private val adaptationStore:
        DynamicScheduleAdaptationPersistenceStore,

    private val replayInputStore:
        DynamicScheduleAdaptationReplayInputStore,

    private val engine:
        DynamicScheduleAdaptationReplayEngine =
        DeterministicDynamicScheduleAdaptationReplayEngine(),
) {

    suspend fun replay(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationReplayResult {

        val record =
            adaptationStore.get(itemId)
                ?: error(
                    "Historical adaptation not found: $itemId",
                )

        val input =
            replayInputStore.get(itemId)
                ?: error(
                    "Historical adaptation replay input " +
                        "not found: $itemId",
                )

        return engine.replay(
            input = input,
            historical =
                listOf(record.adaptation),
        )
    }

    /**
     * 一次重放多个 item。
     *
     * 按 itemId 排序，保证顺序可复现。
     */
    suspend fun replayAll(
        itemIds: Collection<DynamicScheduleItemId>,
    ): Map<
        DynamicScheduleItemId,
        DynamicScheduleAdaptationReplayResult,
        > =
        itemIds
            .distinct()
            .sortedBy { it.value }
            .associateWith { replay(it) }
}
