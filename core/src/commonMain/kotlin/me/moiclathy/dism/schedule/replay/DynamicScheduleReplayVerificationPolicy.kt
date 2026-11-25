package me.moiclathy.dism.schedule.replay

/**
 * 12.28
 *
 * Replay Verification Policy。
 */
data class DynamicScheduleReplayVerificationPolicy(
    /**
     * 是否要求 Engine Version 完全一致。
     *
     * 默认要求。
     */
    val requireSameEngineVersion: Boolean = true,

    /**
     * 是否允许存在 priority 浮点误差。
     */
    val priorityTolerance: Double = 0.000001,

    /**
     * 是否要求 explanation 的存在性一致。
     */
    val requireExplanationParity: Boolean = true,

    /**
     * 是否要求 conflict 的存在性一致。
     */
    val requireConflictParity: Boolean = true,
)
