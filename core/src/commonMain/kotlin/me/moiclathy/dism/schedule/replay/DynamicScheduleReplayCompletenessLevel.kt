package me.moiclathy.dism.schedule.replay

enum class DynamicScheduleReplayCompletenessLevel {

    /**
     * 只有 Snapshot 中保存的结果/摘要。
     */
    SNAPSHOT_RECONSTRUCTION,

    /**
     * 可以使用当前 Runtime 输入重新计算。
     */
    CURRENT_RE_EVALUATION,

    /**
     * 保存了足够的历史 Runtime 输入，
     * 可以进行真正的历史确定性 Replay。
     */
    HISTORICAL_RUNTIME_REPLAY,
}
