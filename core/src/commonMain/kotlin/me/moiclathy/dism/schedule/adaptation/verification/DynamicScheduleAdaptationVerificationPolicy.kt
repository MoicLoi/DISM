package me.moiclathy.dism.schedule.adaptation.verification

/**
 * 14.13
 *
 * Adaptation Verification 的严格程度。
 */
data class DynamicScheduleAdaptationVerificationPolicy(
    /**
     * priority 允许的浮点误差。
     */
    val priorityTolerance: Double = 0.000001,

    /**
     * adaptationFactor 允许的浮点误差。
     */
    val adaptationFactorTolerance: Double = 0.000001,

    /**
     * 是否要求 reasons 完全一致。
     *
     * 默认关闭。
     *
     * 因为 reasons 属于解释层，
     * 不应该轻易把文字变化认定为行为变化。
     *
     * 反过来说：如果打开它，改一句提示语就会产生
     * `BEHAVIOR_CHANGED` 假警报。
     */
    val requireReasonParity: Boolean = false,

    /**
     * 是否要求 Engine Version 一致。
     */
    val requireSameEngineVersion: Boolean = true,

    /**
     * 如果 Policy fingerprint 不同，
     * 是否直接返回 POLICY_CHANGED。
     */
    val requireSamePolicy: Boolean = true,

    /**
     * 如果 Input fingerprint 不同，
     * 是否直接返回 INPUT_CHANGED。
     *
     * 这是 14.13 §十 分类阶梯的第一级：
     * 输入都变了，结果不同根本说明不了算法有问题。
     */
    val requireSameInput: Boolean = true,
) {

    init {
        require(priorityTolerance >= 0.0)
        require(adaptationFactorTolerance >= 0.0)
    }
}
