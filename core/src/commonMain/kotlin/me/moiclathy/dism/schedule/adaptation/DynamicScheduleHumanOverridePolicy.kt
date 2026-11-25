package me.moiclathy.dism.schedule.adaptation

import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleHumanOverridePolicy(
    /**
     * 是否允许用户 override priority。
     */
    val allowPriorityOverride: Boolean = true,

    /**
     * 是否允许用户 override Attention。
     */
    val allowAttentionOverride: Boolean = true,

    /**
     * 用户 override 是否默认永久有效。
     *
     * 14.10 第一版不做自动过期。
     * 未来可以在 14.x 后续阶段加入 expiration。
     */
    val persistentByDefault: Boolean = true,
)
