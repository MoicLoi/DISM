package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.3
 *
 * 多个 Schedule Item 的 Behavioral Adaptation Model。
 *
 * 14.4 很可能不是只处理一个 Item，
 * 所以这里先把容器准备好。
 */
data class DynamicScheduleBehavioralAdaptationSet(
    val adaptations:
        List<DynamicScheduleBehavioralAdaptation> = emptyList(),
) {

    init {
        require(
            adaptations
                .map { it.itemId }
                .distinct()
                .size == adaptations.size,
        ) {
            "adaptation item ids must be unique"
        }
    }

    val isEmpty: Boolean
        get() = adaptations.isEmpty()

    val size: Int
        get() = adaptations.size

    fun find(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleBehavioralAdaptation? =
        adaptations.firstOrNull {
            it.itemId == itemId
        }

    /**
     * 只保留有实际证据的模型。
     */
    val withEvidence:
        List<DynamicScheduleBehavioralAdaptation>
        get() = adaptations.filter { it.hasEvidence }
}

/**
 * 14.3
 *
 * 批量把 Interpretation 转换成 Behavioral Model。
 */
class DynamicScheduleBehavioralAdaptationSetBuilder(
    private val builder:
        DynamicScheduleBehavioralAdaptationBuilder =
        DeterministicDynamicScheduleBehavioralAdaptationBuilder(),
) {

    fun build(
        interpretations:
            Map<
                DynamicScheduleItemId,
                DynamicScheduleDecisionInterpretation,
                >,
    ): DynamicScheduleBehavioralAdaptationSet {

        /*
         * 按 itemId 排序，保证：
         *
         * ```text
         * 相同输入 → 相同输出顺序
         * ```
         *
         * 这对后面的 Replay / Verification / Provenance
         * 都是必须的——顺序不确定的东西没法做指纹。
         */
        val adaptations =
            interpretations
                .entries
                .sortedBy { it.key.value }
                .map { (itemId, interpretation) ->
                    builder.build(
                        itemId = itemId,
                        interpretation = interpretation,
                    )
                }

        return DynamicScheduleBehavioralAdaptationSet(
            adaptations = adaptations,
        )
    }
}

/**
 * 14.3
 *
 * Behavioral Adaptation 服务。
 */
class DynamicScheduleBehavioralAdaptationService(
    private val builder:
        DynamicScheduleBehavioralAdaptationBuilder =
        DeterministicDynamicScheduleBehavioralAdaptationBuilder(),
) {

    fun build(
        itemId: DynamicScheduleItemId,

        interpretation:
            DynamicScheduleDecisionInterpretation,
    ): DynamicScheduleBehavioralAdaptation =
        builder.build(
            itemId = itemId,
            interpretation = interpretation,
        )
}
