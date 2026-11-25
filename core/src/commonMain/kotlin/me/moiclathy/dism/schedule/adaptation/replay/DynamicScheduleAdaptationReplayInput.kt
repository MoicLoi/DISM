package me.moiclathy.dism.schedule.adaptation.replay

import kotlinx.serialization.Serializable

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 14.12
 *
 * 一次 Adaptation Replay 所需要的完整历史输入。
 *
 * 注意：
 * 这里保存的是"输入"，而不是 Adaptation 的结果。
 *
 * 历史结果由 14.11 Persistence 提供。
 *
 * ### 为什么 policy 也必须是历史输入
 *
 * 如果不存 policy，就会出现：
 *
 * ```text
 * 历史：factor = 1.25
 * 重放：factor = 1.10
 * ```
 *
 * 而真正原因只是"policy 已经被用户改过"，
 * 不是算法退化。把 policy 一起存下来之后，
 * 重放差异才真正等价于"算法变了"。
 *
 * ### 为什么 `histories` 的 key 是 String
 *
 * 它对应 12.x 的 `AttentionKey` / 稳定 item identity，
 * 与 `DynamicScheduleItemId` 是不同的东西：
 * 一个 item 可能对应多个 attention key。
 */
@Serializable
data class DynamicScheduleAdaptationReplayInput(
    /**
     * 当时参与计算的 Schedule Model。
     */
    val model: DynamicScheduleModel,

    /**
     * 当时参与计算的 Attention History。
     *
     * key = stable AttentionKey / item identity
     */
    val histories:
        Map<String, DynamicScheduleAttentionHistory> =
        emptyMap(),

    /**
     * 当时使用的 Priority Adaptation Policy。
     */
    val policy:
        DynamicSchedulePriorityAdaptationPolicy =
        DynamicSchedulePriorityAdaptationPolicy(),
)
