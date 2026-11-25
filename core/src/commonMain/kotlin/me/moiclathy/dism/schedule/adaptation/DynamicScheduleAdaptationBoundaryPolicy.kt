package me.moiclathy.dism.schedule.adaptation

/**
 * 14.9
 *
 * 控制自动 Adaptation 的最大行为范围。
 */
data class DynamicScheduleAdaptationBoundaryPolicy(
    /**
     * 单次自动调整最大绝对值。
     *
     * 例如 0.10：
     *
     *     requested +0.30
     *     → maximum +0.10
     */
    val maximumSingleDelta: Double = 0.10,

    /**
     * 累计自动调整最大绝对值。
     *
     * 例如：
     *
     *     当前已经自动 +0.15
     *     最大累计 +0.20
     *
     * 那么本次最多再 +0.05。
     */
    val maximumCumulativeDelta: Double = 0.20,

    /**
     * 时间窗口。
     *
     * 用于限制一段时间内的自动调整。
     */
    val windowMillis: Long = 24L * 60L * 60L * 1000L,

    /**
     * 时间窗口内最大自动调整次数。
     */
    val maximumChangesPerWindow: Int = 3,

    /**
     * Confidence 低于这个值时，
     * 不允许自动 Adaptation。
     */
    val minimumConfidence: Double = 0.50,

    /**
     * Confidence 很低时完全禁用。
     */
    val hardMinimumConfidence: Double = 0.25,

    /**
     * 是否保护用户明确设置的 Priority。
     */
    val protectExplicitUserSetting: Boolean = true,
) {
    init {
        require(maximumSingleDelta >= 0.0)
        require(maximumCumulativeDelta >= 0.0)
        require(windowMillis > 0L)
        require(maximumChangesPerWindow >= 0)
        require(minimumConfidence in 0.0..1.0)
        require(hardMinimumConfidence in 0.0..1.0)

        require(
            hardMinimumConfidence <=
                minimumConfidence,
        )
    }
}
