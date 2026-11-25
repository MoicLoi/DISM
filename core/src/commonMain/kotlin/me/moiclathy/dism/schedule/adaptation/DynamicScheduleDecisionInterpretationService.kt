package me.moiclathy.dism.schedule.adaptation

/**
 * 14.2
 *
 * ```text
 * FeatureSet
 *     ↓
 * InterpretationService
 *     ↓
 * Interpreter
 * ```
 *
 * 多这一层的意义：以后 15.x 想换成统计模型 / ML / LLM，
 * 只需要换 [DynamicScheduleDecisionFeatureInterpreter] 的实现，
 * 上层调用点完全不动。
 */
class DynamicScheduleDecisionInterpretationService(
    private val interpreter:
        DynamicScheduleDecisionFeatureInterpreter =
        DeterministicDynamicScheduleDecisionFeatureInterpreter(),
) {

    fun interpret(
        features: DynamicScheduleDecisionFeatureSet,
    ): DynamicScheduleDecisionInterpretation =
        interpreter.interpret(features)
}
