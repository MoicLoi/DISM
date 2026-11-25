package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleAttentionHistory(
    val attentionKey: String,

    /**
     * 用户实际看到该信息的次数。
     */
    val shownCount: Int = 0,

    /**
     * 用户主动打开/查看的次数。
     */
    val engagedCount: Int = 0,

    /**
     * 用户主动忽略的次数。
     *
     * 注意：
     * ignored != disliked。
     */
    val ignoredCount: Int = 0,

    /**
     * 用户主动隐藏/拒绝的次数。
     */
    val dismissedCount: Int = 0,

    /**
     * 用户主动要求提醒/重新查看的次数。
     */
    val requestedCount: Int = 0,

    /**
     * 最近一次行为时间。
     */
    val lastInteractionAt: Instant? = null,

    /**
     * 最近一次有效反馈。
     *
     * -1.0 = 强烈降低注意力
     *  0.0 = 无反馈
     * +1.0 = 强烈提高注意力
     */
    val feedbackSignal: Double = 0.0,
) {
    init {
        require(attentionKey.isNotBlank())
        require(shownCount >= 0)
        require(engagedCount >= 0)
        require(ignoredCount >= 0)
        require(dismissedCount >= 0)
        require(requestedCount >= 0)
        require(feedbackSignal in -1.0..1.0)
    }
}
