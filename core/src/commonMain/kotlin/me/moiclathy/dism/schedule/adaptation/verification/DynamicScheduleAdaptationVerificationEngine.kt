package me.moiclathy.dism.schedule.adaptation.verification

import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptation

/**
 * 14.13
 *
 * ```text
 * 历史结果 + 重放结果 + 版本/指纹 → 结论
 * ```
 *
 * 它是纯函数：不读时钟、不读数据库。
 */
interface DynamicScheduleAdaptationVerificationEngine {

    fun verify(
        historical:
            List<DynamicSchedulePriorityAdaptation>,

        replayed:
            List<DynamicSchedulePriorityAdaptation>,

        historicalEngineVersion: String,

        replayEngineVersion: String,

        historicalPolicyFingerprint: String,

        replayPolicyFingerprint: String,

        policy:
            DynamicScheduleAdaptationVerificationPolicy =
            DynamicScheduleAdaptationVerificationPolicy(),

        /**
         * 14.13 §十：输入指纹。
         *
         * 默认 null = 调用方没提供，跳过 INPUT_CHANGED 这一级。
         */
        historicalInputFingerprint: String? = null,

        replayInputFingerprint: String? = null,
    ): DynamicScheduleAdaptationVerificationReport
}
