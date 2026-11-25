package me.moiclathy.dism.schedule.adaptation

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 用户对自动适配结果施加的显式覆盖。
 *
 * Human Override 的语义优先级高于：
 *
 * - 历史行为
 * - Context Adaptation
 * - Confidence
 * - Boundary
 * - 自动 Priority Adaptation
 *
 * 但它本身不修改原始 Schedule Item。
 *
 * 14.11 起需要 `@Serializable`：它随适配记录一起落盘，
 * 这样重启后仍然能区分"自动算出来"和"用户手动设的"。
 */
@Serializable
data class DynamicScheduleHumanOverride(
    val itemId: DynamicScheduleItemId,

    /**
     * 用户明确指定的最终 priority。
     *
     * null 表示用户没有覆盖 priority。
     */
    val priorityOverride: Double? = null,

    /**
     * 用户是否明确要求该项参与 Attention。
     *
     * null = 不覆盖自动 Attention 行为
     * true = 强制允许
     * false = 强制禁止
     */
    val attentionOverride: Boolean? = null,

    /**
     * 是否锁定该 override。
     *
     * 当前 14.10 中只允许 active override 生效。
     */
    val locked: Boolean = true,

    /**
     * 用户产生该 override 的时间。
     */
    val createdAt: Instant,

    /**
     * 可选的人类可读原因。
     */
    val reason: String? = null,
) {
    init {
        priorityOverride?.let {
            require(it in 0.0..1.0)
        }

        require(reason == null || reason.isNotBlank())
    }

    val hasPriorityOverride: Boolean
        get() = priorityOverride != null

    val hasAttentionOverride: Boolean
        get() = attentionOverride != null

    val isActive: Boolean
        get() = locked
}
