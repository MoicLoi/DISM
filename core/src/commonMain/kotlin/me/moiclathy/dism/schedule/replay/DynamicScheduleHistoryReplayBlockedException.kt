package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityResult

/**
 * 13.10
 *
 * 历史版本不兼容时，Replay 被明确拒绝。
 *
 * 这不是"出错"，而是一个**正常的业务结论**：
 *
 * ```text
 * 历史引擎版本：13.1
 * 当前引擎版本：14.0
 * Compatibility：INCOMPATIBLE
 * → 这条历史不能用当前语义重放
 * ```
 *
 * 之所以用异常而不是返回一个"假的结果对象"：
 * [DynamicScheduleHistoricalRuntimeReplayResult] 的核心字段是
 * `replayedResult`，而"被拒绝"的情况下根本不存在这个结果。
 * 硬塞一个占位值只会让调用方以为重放发生过。
 *
 * 想避免异常处理的调用方应该先查
 * [DynamicScheduleHistoryVersionDiagnostic]。
 */
class DynamicScheduleHistoryReplayBlockedException(
    val compatibility:
        DynamicScheduleHistoryVersionCompatibilityResult,
) : IllegalStateException(
    buildString {
        append("Historical replay is blocked. ")
        append("Historical engine version: ")
        append(compatibility.historicalVersion.value)
        append(", current engine version: ")
        append(compatibility.currentVersion.value)
        append(", compatibility: ")
        append(compatibility.compatibility)
        append(". ")
        append(compatibility.reason)
    },
)
