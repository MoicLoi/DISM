package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.3
 *
 * 把 14.2 的 Interpretation 转换成 Behavioral Adaptation Model。
 *
 * 设计成接口是为了以后能替换实现：
 *
 * ```text
 * DeterministicBehavioralAdaptationBuilder
 * StatisticalBehavioralAdaptationBuilder
 * ExperimentalBehavioralAdaptationBuilder
 * ```
 *
 * 而不用重写 Session。
 */
interface DynamicScheduleBehavioralAdaptationBuilder {

    fun build(
        itemId: DynamicScheduleItemId,

        interpretation:
            DynamicScheduleDecisionInterpretation,
    ): DynamicScheduleBehavioralAdaptation
}
