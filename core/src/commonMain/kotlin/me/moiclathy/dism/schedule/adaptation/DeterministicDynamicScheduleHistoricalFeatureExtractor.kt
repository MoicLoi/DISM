package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryEntry
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryQuery
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryService
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

/**
 * 14.1
 *
 * 从历史里**测量**出结构化特征。
 *
 * ### 第一版只有 5 个特征
 *
 * ```text
 * completion_rate
 * postponement_rate
 * cancellation_rate
 * decision_stability
 * recent_activity
 * ```
 *
 * 设计稿原本还有第 6 个 `recent_occurrence_rate`，但它被自己删掉了，
 * 理由很对：`history.query(itemId = A)` 本身已经意味着
 * "这个 item 在当前窗口内至少出现过一次"，
 * 把它写成 `8 / 10` 在数学上没有意义——
 * 没有参考窗口就谈不上"出现频率"。
 *
 * 等以后有明确窗口（last 7 days / last N occurrences）
 * 再加入 `occurrence_density`。
 *
 * ### 数据来源
 *
 * 直接用现有的 [DynamicScheduleDecisionHistoryService]，
 * 不复制任何 Item / Snapshot 模型（设计稿 §14）。
 */
class DeterministicDynamicScheduleHistoricalFeatureExtractor(
    private val history:
        DynamicScheduleDecisionHistoryService,
) : DynamicScheduleHistoricalFeatureExtractor {

    override suspend fun extract(
        itemId: DynamicScheduleItemId,

        limit: Int,

        now: Instant,
    ): DynamicScheduleDecisionFeatureSet {

        require(limit > 0) {
            "limit must be positive"
        }

        val result =
            history.query(
                DynamicScheduleDecisionHistoryQuery(
                    itemId = itemId,
                    limit = limit,

                    /*
                     * 特征提取只读 Snapshot 历史，
                     * 不需要 Trace 内容，更不允许触发重放。
                     */
                    includeTrace = false,
                    includeProvenance = false,
                    includeReplay = false,
                    includeVerification = false,
                ),
            )

        val entries = result.entries

        if (entries.isEmpty()) {
            return DynamicScheduleDecisionFeatureSet.EMPTY
        }

        val sampleCount = entries.size

        /*
         * 历史条目按 capturedAt 降序返回，
         * 所以 latest 就是第一个。
         */
        val latest =
            entries.maxBy { it.capturedAt }

        return DynamicScheduleDecisionFeatureSet(
            features =
                listOf(
                    feature(
                        key = "completion_rate",

                        value =
                            rate(
                                entries,
                                DynamicScheduleState.COMPLETED,
                            ),

                        sampleCount = sampleCount,

                        source =
                            DynamicScheduleDecisionFeatureSource
                                .SNAPSHOT_HISTORY,

                        observedAt = latest.capturedAt,
                    ),

                    feature(
                        key = "postponement_rate",

                        value =
                            rate(
                                entries,
                                DynamicScheduleState.POSTPONED,
                            ),

                        sampleCount = sampleCount,

                        source =
                            DynamicScheduleDecisionFeatureSource
                                .SNAPSHOT_HISTORY,

                        observedAt = latest.capturedAt,
                    ),

                    feature(
                        key = "cancellation_rate",

                        value =
                            rate(
                                entries,
                                DynamicScheduleState.CANCELLED,
                            ),

                        sampleCount = sampleCount,

                        source =
                            DynamicScheduleDecisionFeatureSource
                                .SNAPSHOT_HISTORY,

                        observedAt = latest.capturedAt,
                    ),

                    feature(
                        key = "decision_stability",

                        value =
                            decisionStability(entries),

                        sampleCount = sampleCount,

                        /*
                         * 稳定性描述的是"决策结果"的波动，
                         * 所以来源是 Trace 层语义
                         * （决策链路本身的产出）。
                         */
                        source =
                            DynamicScheduleDecisionFeatureSource
                                .TRACE_HISTORY,

                        observedAt = latest.capturedAt,
                    ),

                    feature(
                        key = "recent_activity",

                        value =
                            recentActivity(
                                latest.capturedAt,
                                now,
                            ),

                        sampleCount = sampleCount,

                        source =
                            DynamicScheduleDecisionFeatureSource
                                .SNAPSHOT_HISTORY,

                        observedAt = latest.capturedAt,
                    ),
                ),
        )
    }

    /**
     * 某个状态在窗口内出现的比例。
     */
    private fun rate(
        entries: List<DynamicScheduleDecisionHistoryEntry>,
        state: DynamicScheduleState,
    ): Double =
        entries.count {
            it.snapshot.item.state == state
        }.toDouble() / entries.size

    /**
     * 决策结果是否稳定。
     *
     * ```text
     * stability = 1 - normalizedVariance
     *
     * 0.0 = 非常不稳定
     * 1.0 = 非常稳定
     * ```
     *
     * 用的是 Snapshot 里记录下来的**决策优先级**——
     * 也就是"当时系统最终怎么看待这件事"，
     * 而不是输入模型的原始 priority。
     */
    private fun decisionStability(
        entries: List<DynamicScheduleDecisionHistoryEntry>,
    ): Double {

        if (entries.size <= 1) {
            return 1.0
        }

        val priorities =
            entries.map {
                it.snapshot.item.priority
            }

        val mean = priorities.average()

        val variance =
            priorities
                .map {
                    val delta = it - mean
                    delta * delta
                }
                .average()

        /*
         * priority 本身就在 [0, 1]，
         * 因此方差最大约为 0.25；这里按 0.25 归一化，
         * 让"优先级在 0 和 1 之间来回跳"对应 0.0。
         */
        val normalizedVariance =
            (variance / 0.25).coerceIn(0.0, 1.0)

        return (1.0 - normalizedVariance)
            .coerceIn(0.0, 1.0)
    }

    /**
     * 这个 item 最近是否还活跃。
     *
     * 复用 [DynamicScheduleRecencyDecay]，不重新发明衰减。
     */
    private fun recentActivity(
        lastObservedAt: Instant,
        now: Instant,
    ): Double =
        DynamicScheduleRecencyDecay.weight(
            elapsed = now - lastObservedAt,
            policy = DynamicScheduleAttentionDecayPolicy(),
        )

    private fun feature(
        key: String,
        value: Double,
        sampleCount: Int,
        source: DynamicScheduleDecisionFeatureSource,
        observedAt: Instant,
    ): DynamicScheduleDecisionFeature =
        DynamicScheduleDecisionFeature(
            key = key,
            value = value.coerceIn(0.0, 1.0),
            confidence = confidenceFor(sampleCount),
            observedAt = observedAt,
            sampleCount = sampleCount,
            source = source,
        )

    /**
     * 样本越少越不可信。
     */
    private fun confidenceFor(
        sampleCount: Int,
    ): Double =
        (sampleCount / 10.0).coerceIn(0.0, 1.0)
}
