package me.moiclathy.dism.schedule.adaptation

/**
 * 14.5
 *
 * 批量生成 Attention Adaptation。
 *
 * ### 为什么返回 `List` 而不是 Set
 *
 * 与 14.4 的 `DynamicScheduleWeightAdaptationSet` 不同，
 * 这里保持规格里的 `List`：
 *
 * ```text
 * Adaptation 只是一个中间产物，
 * 还没有 itemId 唯一性保证的需求。
 * ```
 *
 * 结果仍然按 itemId 排序，保证相同输入得到相同顺序。
 *
 * ### 缺少对应 Weight Adaptation 时
 *
 * 返回 `enabled = false` 的条目，**不是**直接跳过。
 * 跳过会让"这个 item 没有适配"和"忘了算这个 item"
 * 变得无法区分。
 */
class DynamicScheduleAttentionAdaptationService(
    private val adapter:
        DynamicScheduleAttentionAdapter =
        DeterministicDynamicScheduleAttentionAdapter(),
) {

    fun adapt(
        behavioralModels:
            List<DynamicScheduleBehavioralAdaptation>,

        weightAdaptations:
            List<DynamicScheduleWeightAdaptation>,

        policy:
            DynamicScheduleAttentionAdaptationPolicy =
            DynamicScheduleAttentionAdaptationPolicy(),
    ): List<DynamicScheduleAttentionAdaptation> {

        val weightByItem =
            weightAdaptations.associateBy {
                it.itemId
            }

        return behavioralModels
            .sortedBy {
                it.itemId.value.toString()
            }
            .map { behavioral ->

                val weight =
                    weightByItem[
                        behavioral.itemId
                    ]

                if (weight == null) {
                    DynamicScheduleAttentionAdaptation(
                        itemId =
                            behavioral.itemId,

                        attentionDelta = 0.0,

                        confidence =
                            behavioral.confidence,

                        sampleCount =
                            behavioral.sampleCount,

                        enabled = false,
                    )
                } else {
                    adapter.adapt(
                        behavioral = behavioral,
                        weightAdaptation = weight,
                        policy = policy,
                    )
                }
            }
    }

    /**
     * 单条适配。
     */
    fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        weightAdaptation:
            DynamicScheduleWeightAdaptation,

        policy:
            DynamicScheduleAttentionAdaptationPolicy =
            DynamicScheduleAttentionAdaptationPolicy(),
    ): DynamicScheduleAttentionAdaptation =
        adapter.adapt(
            behavioral = behavioral,
            weightAdaptation = weightAdaptation,
            policy = policy,
        )
}
