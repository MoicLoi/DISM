package me.moiclathy.dism.schedule.adaptation.replay

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.12
 *
 * Replay Input 的持久化端口。
 *
 * 它和 14.11 的适配结果分开存：
 *
 * ```text
 * schedule_adaptation             → "当时算出了什么"
 * schedule_adaptation_replay_input → "当时用什么算的"
 * ```
 *
 * 两者一一对应，但**不能合成一张表**——
 * 结果会被后续自动调整覆盖，输入必须保持原样。
 */
interface DynamicScheduleAdaptationReplayInputStore {

    suspend fun save(
        itemId: DynamicScheduleItemId,
        input: DynamicScheduleAdaptationReplayInput,
    )

    suspend fun get(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationReplayInput?

    suspend fun delete(
        itemId: DynamicScheduleItemId,
    )

    suspend fun clear()
}
