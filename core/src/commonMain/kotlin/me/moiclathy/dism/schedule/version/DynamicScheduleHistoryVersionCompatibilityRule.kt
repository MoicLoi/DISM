package me.moiclathy.dism.schedule.version

/**
 * 13.10.8
 *
 * 一条**显式**的版本兼容性规则。
 *
 * 关键设计：兼容性必须由人明确注册，
 * 而不是从版本号推断。
 *
 * ```text
 * 千万不要：historical.startsWith("13.") → 兼容
 * ```
 *
 * 因为那只是字符串关系。真正需要确认的是：
 *
 * ```text
 * ReplayInput schema
 *      +
 * Runtime semantics
 *      +
 * Decision calculation
 * ```
 *
 * 三者是否仍然一致。
 */
interface DynamicScheduleHistoryVersionCompatibilityRule {

    fun matches(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,

        currentVersion:
            DynamicScheduleHistoryEngineVersion,
    ): Boolean

    fun resolve(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,

        currentVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryVersionCompatibilityResult
}
