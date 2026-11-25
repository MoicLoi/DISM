package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.4
 *
 * 批量进行 Dynamic Weight Adaptation。
 *
 * ### 它不写任何东西
 *
 * 这里产出的只是"权重应该怎么动"，
 * 真正落到决策上发生在 14.5+，持久化发生在 14.11。
 */
class DynamicScheduleWeightAdaptationService(
    private val adapter:
        DynamicScheduleWeightAdapter =
        DeterministicDynamicScheduleWeightAdapter(),
) {

    /**
     * 批量适配。
     *
     * 按 itemId 排序，保证：
     *
     * ```text
     * 相同输入 → 相同输出顺序
     * ```
     *
     * 这是后面做 Replay / 指纹 / Provenance 的前提。
     */
    fun adapt(
        behavioralModels:
            List<DynamicScheduleBehavioralAdaptation>,

        policy:
            DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),
    ): DynamicScheduleWeightAdaptationSet {

        val adaptations =
            behavioralModels
                .sortedBy {
                    it.itemId.value.toString()
                }
                .map {
                    adapter.adapt(
                        behavioral = it,
                        policy = policy,
                    )
                }

        return DynamicScheduleWeightAdaptationSet(
            adaptations = adaptations,
        )
    }

    /**
     * 单条适配。
     *
     * 会话层的 `weightAdaptation(itemId)` 走这条路径；
     * 语义与批量版本完全一致。
     */
    fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        policy:
            DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),
    ): DynamicScheduleWeightAdaptation =
        adapter.adapt(
            behavioral = behavioral,
            policy = policy,
        )
}
