package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleAdaptationBoundaryState(
    val itemId: DynamicScheduleItemId,

    /**
     * 当前累计自动调整量。
     */
    val cumulativeDelta: Double = 0.0,

    /**
     * 最近自动调整时间。
     */
    val recentChangeTimes: List<Instant> = emptyList(),

    /**
     * 用户是否明确设置过该项。
     */
    val explicitUserSetting: Boolean = false,

    /**
     * 用户最后一次明确设置时间。
     */
    val explicitUserSettingAt: Instant? = null,
) {
    init {
        require(cumulativeDelta.isFinite())
    }
}
