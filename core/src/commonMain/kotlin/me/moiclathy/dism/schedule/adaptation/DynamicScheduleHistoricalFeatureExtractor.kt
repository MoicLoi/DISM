package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.1
 *
 * ```text
 * Historical Evidence
 *        ↓
 * Feature Extractor
 *        ↓
 * Decision Feature Set
 * ```
 *
 * ### 它是只读的
 *
 * 这个接口**不修改**任何历史，也**不修改**任何 Weight。
 *
 * ```text
 * History → Features        ← 14.1 到这里结束
 * Features → Dynamic Weight ← 14.2 之后才研究
 * ```
 *
 * 因为 `completion_rate = 0.9` 并不能直接推出
 * "应该提高 priority"——那属于解释，不属于测量。
 */
interface DynamicScheduleHistoricalFeatureExtractor {

    suspend fun extract(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        /**
         * 计算"最近活跃度"所需的当前时间。
         *
         * 由调用方传入，不在底层读系统时钟：
         * 同样的历史必须得到同样的特征。
         */
        now: Instant,
    ): DynamicScheduleDecisionFeatureSet
}
