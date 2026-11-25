package me.moiclathy.dism.schedule.adaptation.verification

/**
 * 14.13
 *
 * Adaptation Verification 的最终状态。
 */
enum class DynamicScheduleAdaptationVerificationStatus {

    /**
     * 历史 Adaptation 与 Replay 完全一致。
     */
    EXACT_MATCH,

    /**
     * Replay 所使用的历史输入与当前输入不同。
     *
     * 因此即使结果不同，也不能直接认为 Adaptation Engine
     * 的行为发生了变化。
     */
    INPUT_CHANGED,

    /**
     * Policy 发生变化。
     */
    POLICY_CHANGED,

    /**
     * Adaptation Engine / Algorithm Version 发生变化。
     */
    ENGINE_CHANGED,

    /**
     * 输入与 Engine 条件均满足，但最终 Adaptation 发生变化。
     */
    BEHAVIOR_CHANGED,

    /**
     * Verification 本身无法正常完成。
     */
    VERIFICATION_FAILED,
}
