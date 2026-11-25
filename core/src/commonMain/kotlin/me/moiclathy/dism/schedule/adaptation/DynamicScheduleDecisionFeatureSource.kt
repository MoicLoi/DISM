package me.moiclathy.dism.schedule.adaptation

/**
 * 14.1
 *
 * 这个 Feature 是从哪一类历史事实里提取出来的。
 *
 * 保留来源不是学术洁癖，而是为了后面能回答：
 *
 * ```text
 * 这个结论是"当时系统怎么看"（Snapshot）？
 * 还是"当时为什么这么判断"（Trace）？
 * 还是"后来重放是否一致"（Replay）？
 * 还是"用户过去怎么对待提醒"（Attention Memory）？
 * ```
 *
 * 不同来源的可信度、时效性、可解释性都不一样。
 */
enum class DynamicScheduleDecisionFeatureSource {

    /**
     * 来自历史 Decision Snapshot（当时发生过什么）。
     */
    SNAPSHOT_HISTORY,

    /**
     * 来自历史 Decision Trace（当时为什么这样决定）。
     */
    TRACE_HISTORY,

    /**
     * 来自 Replay / Verification（后来重跑是否一致）。
     */
    REPLAY_HISTORY,

    /**
     * 来自 Attention Memory（用户过去怎么对待提醒）。
     */
    ATTENTION_MEMORY,
}
