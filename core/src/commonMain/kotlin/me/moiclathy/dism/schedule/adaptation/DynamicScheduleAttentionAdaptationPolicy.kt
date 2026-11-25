package me.moiclathy.dism.schedule.adaptation

/**
 * 14.5
 *
 * Attention Adaptation 的安全边界。
 */
data class DynamicScheduleAttentionAdaptationPolicy(
    /**
     * Attention 单次最大变化量。
     */
    val maximumDelta: Double = 0.20,

    /**
     * 最低置信度。
     */
    val minimumConfidence: Double = 0.50,

    /**
     * 最少历史样本。
     */
    val minimumSampleCount: Int = 3,

    /**
     * 最近活动对 Attention 的敏感度。
     */
    val recencySensitivity: Double = 1.0,

    /**
     * 完成倾向对 Attention 的敏感度。
     *
     * 第一版默认 0。
     *
     * 原因：
     *
     * ```text
     * "经常完成"本身并不代表应该更频繁打扰用户。
     * ```
     *
     * 反过来说，如果"经常完成"就直接提高 Attention，
     * 用户会发现自己**最不需要提醒的事情**被提醒得最多。
     */
    val completionSensitivity: Double = 0.0,

    /**
     * 推迟倾向对 Attention 的敏感度。
     */
    val postponementSensitivity: Double = 0.5,

    /**
     * 取消倾向对 Attention 的敏感度。
     *
     * 默认 0。
     *
     * 取消历史不能简单等价为"以后少提醒"：
     * 取消可能是因为"这件事确实不重要"，
     * 也可能是因为"这件事总是和我已有的安排冲突"，
     * 后者恰恰需要更多 Attention。
     */
    val cancellationSensitivity: Double = 0.0,
) {

    init {
        require(maximumDelta >= 0.0)
        require(minimumConfidence in 0.0..1.0)
        require(minimumSampleCount >= 0)

        require(recencySensitivity >= 0.0)
        require(completionSensitivity >= 0.0)
        require(postponementSensitivity >= 0.0)
        require(cancellationSensitivity >= 0.0)
    }
}
