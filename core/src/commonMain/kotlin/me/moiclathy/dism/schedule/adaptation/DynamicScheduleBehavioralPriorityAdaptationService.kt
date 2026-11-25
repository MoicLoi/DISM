package me.moiclathy.dism.schedule.adaptation

/**
 * 14.6
 *
 * 批量生成 Schedule Priority Adaptation。
 *
 * 缺少对应 Weight Adaptation 时返回 `enabled = false` 的条目，
 * **不是**直接跳过——跳过会让"没有适配"和"忘了算"无法区分。
 */
class DynamicScheduleBehavioralPriorityAdaptationService(
    private val adapter:
        DynamicScheduleBehavioralPriorityAdaptationAdapter =
        DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter(),
) {

    fun adapt(
        behavioralModels:
            List<DynamicScheduleBehavioralAdaptation>,

        weightAdaptations:
            List<DynamicScheduleWeightAdaptation>,

        policy:
            DynamicScheduleBehavioralPriorityAdaptationPolicy =
            DynamicScheduleBehavioralPriorityAdaptationPolicy(),
    ): List<DynamicScheduleBehavioralPriorityAdaptation> {

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
                    DynamicScheduleBehavioralPriorityAdaptation(
                        itemId =
                            behavioral.itemId,

                        priorityDelta = 0.0,

                        confidence =
                            behavioral.confidence,

                        sampleCount =
                            behavioral.sampleCount,

                        enabled = false,
                    )
                } else {
                    adapter.adapt(
                        behavioral =
                            behavioral,

                        weightAdaptation =
                            weight,

                        policy =
                            policy,
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
            DynamicScheduleBehavioralPriorityAdaptationPolicy =
            DynamicScheduleBehavioralPriorityAdaptationPolicy(),
    ): DynamicScheduleBehavioralPriorityAdaptation =
        adapter.adapt(
            behavioral = behavioral,
            weightAdaptation = weightAdaptation,
            policy = policy,
        )
}
