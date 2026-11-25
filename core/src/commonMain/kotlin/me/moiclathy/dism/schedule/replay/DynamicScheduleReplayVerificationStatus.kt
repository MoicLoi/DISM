package me.moiclathy.dism.schedule.replay

/**
 * 12.28
 *
 * Historical Replay Verification 状态。
 */
enum class DynamicScheduleReplayVerificationStatus {

    /**
     * 历史 Replay 与保存的历史决策一致。
     */
    EXACT_MATCH,

    /**
     * Replay 成功执行，但结果与历史决策存在差异。
     */
    BEHAVIOR_CHANGED,

    /**
     * 历史 Runtime Input 不完整。
     */
    INPUT_INCOMPLETE,

    /**
     * Replay 使用的 Engine Version 与历史版本不同。
     *
     * 这不一定代表 Runtime 有 Bug，
     * 但意味着不能直接把结果视为严格同版本重放。
     */
    ENGINE_VERSION_MISMATCH,

    /**
     * Replay 本身执行失败。
     */
    VERIFICATION_FAILED,
}
