package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.7
 *
 * 将：
 *
 * ```text
 * 历史 Priority Adaptation
 * +
 * 当前 Context Influence
 * ```
 *
 * 合并成当前有效 Priority。
 *
 * 注意：
 * 不修改原始 Schedule Item。
 *
 * ### 两条链在这里汇合
 *
 * ```text
 * 12.x  Attention 历史 → DynamicSchedulePriorityAdaptation
 *                                │
 *                                ▼
 * 14.7                     × context.contextFactor
 *                                │
 *                                ▼
 *                    DynamicScheduleContextSensitivePriority
 * ```
 *
 * `historicalFactor` 与 `contextFactor` 都保留在结果里，
 * 这样"为什么这条现在排在这里"可以拆开回答：
 *
 * ```text
 * 历史原因    historicalFactor
 * 当前场景    contextFactor
 * ```
 */
data class DynamicScheduleContextSensitivePriority(
    val itemId: DynamicScheduleItemId,

    val basePriority: Double,

    val historicalFactor: Double,

    val contextFactor: Double,

    val effectivePriority: Double,

    val contextInfluence:
        DynamicScheduleContextPriorityInfluence,

    val reasons: List<String> = emptyList(),
) {

    init {
        require(basePriority in 0.0..1.0)
        require(historicalFactor >= 0.0)
        require(contextFactor >= 0.0)
        require(effectivePriority in 0.0..1.0)
    }

    companion object {

        fun from(
            adaptation: DynamicSchedulePriorityAdaptation,
            context:
                DynamicScheduleContextPriorityInfluence,
        ): DynamicScheduleContextSensitivePriority {

            val effective =
                (
                    adaptation.effectivePriority *
                        context.contextFactor
                    )
                    .coerceIn(0.0, 1.0)

            return DynamicScheduleContextSensitivePriority(
                itemId =
                    adaptation.itemId,

                basePriority =
                    adaptation.basePriority,

                historicalFactor =
                    adaptation.adaptationFactor,

                contextFactor =
                    context.contextFactor,

                effectivePriority =
                    effective,

                contextInfluence =
                    context,

                reasons =
                    adaptation.reasons +
                        context.reasons,
            )
        }
    }
}
