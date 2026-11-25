package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult

/**
 * 13.15.19
 *
 * **行为**指纹。
 *
 * 只由三样东西决定：
 *
 * ```text
 * ReplayInput
 *      +
 * Engine Version
 *      +
 * Replay Result
 * ```
 *
 * 刻意**不包含** [DynamicScheduleReplayExecutionId.replayedAt]。
 *
 * ### 为什么这条区分很重要
 *
 * 否则：
 *
 * ```text
 * 同一条历史
 * 14:00 重放 → fingerprint A
 * 15:00 重放 → fingerprint B
 * ```
 *
 * 会天然得到两个不同指纹，于是"行为有没有变"这个问题
 * 永远回答"变了"——而这个结论毫无信息量。
 *
 * 正确的语义是：
 *
 * ```text
 * 同样输入 + 同样 Engine + 同样评估时间
 *      = 同样 Behavior Fingerprint
 * ```
 *
 * 不同 [DynamicScheduleReplayExecutionId.replayedAt] 不影响它。
 *
 * 这样 12.29 建立的 Determinism 才在时间维度上真正闭合。
 */
data class DynamicScheduleReplayBehaviorFingerprint(
    val snapshotId: String,

    val engineVersion: String,

    /**
     * 历史评估时间。
     *
     * 它是**输入的一部分**（Runtime 用 now 参与决策），
     * 所以必须进入指纹；这与"重放执行时间"是两件事。
     */
    val evaluatedAt: kotlin.time.Instant,

    val fingerprint: String,
) {

    init {
        require(snapshotId.isNotBlank())
        require(engineVersion.isNotBlank())
        require(fingerprint.isNotBlank())
    }

    companion object {

        fun from(
            snapshotId: String,

            engineVersion: String,

            evaluatedAt: kotlin.time.Instant,

            result: DynamicScheduleIntelligenceResult,
        ): DynamicScheduleReplayBehaviorFingerprint =
            DynamicScheduleReplayBehaviorFingerprint(
                snapshotId = snapshotId,

                engineVersion = engineVersion,

                evaluatedAt = evaluatedAt,

                fingerprint =
                    DynamicScheduleReplayDeterminismFingerprint
                        .from(result)
                        .canonical,
            )
    }
}
