package me.moiclathy.dism.schedule.version

/**
 * 13.10.5
 *
 * 版本兼容性注册表。
 *
 * 不要把版本关系硬编码到 Replay Engine 里——
 * 否则每加一个版本都要改历史重放核心。
 */
interface DynamicScheduleHistoryVersionCompatibilityRegistry {

    fun resolve(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,

        currentVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryVersionCompatibilityResult
}
