package me.moiclathy.dism.schedule.adaptation

/**
 * 14.2
 *
 * 把 14.1 的结构化历史 Feature 转换成语义解释。
 *
 * **它不修改任何 Schedule / Priority / Attention / 持久化历史。**
 *
 * 具体的解释器以后可以换成统计模型、ML 或 LLM，
 * 而上层只依赖这个接口。
 */
interface DynamicScheduleDecisionFeatureInterpreter {

    fun interpret(
        features: DynamicScheduleDecisionFeatureSet,
    ): DynamicScheduleDecisionInterpretation
}
