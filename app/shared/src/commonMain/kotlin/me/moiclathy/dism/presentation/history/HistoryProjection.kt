package me.moiclathy.dism.presentation.history

import kotlinx.datetime.TimeZone

import me.moiclathy.dism.presentation.home.TimeFormatting
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryEntry
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryResult
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.8：`History API -> HistoryUiState` 的纯映射。
 */
fun historyItems(
    model: DynamicScheduleModel,
): List<HistoryItemUi> =
    model.items
        .map {
            HistoryItemUi(
                itemId = it.id.value,
                title = it.title,
            )
        }
        .sortedBy { it.itemId }

/**
 * 历史条目 → 展示形态。
 *
 * ### 排序按 `capturedAt`，**不能**按格式化后的字符串
 *
 * 一个真实的陷阱：
 *
 * ```text
 * "Wed, Sep 9 08:00"
 * "Tue, Aug 4 09:00"
 * ```
 *
 * 这两个字符串按字典序比较时，先比的是**星期缩写**，
 * 而不是日期。于是 "Wed" 会排在 "Tue" 之后，
 * 跨月跨周的历史顺序会完全错乱。
 *
 * 所以这里先在领域对象上按 `capturedAt` 排序，
 * 再转成展示字符串。格式化只负责显示，不承担排序。
 */
fun historyEntries(
    result: DynamicScheduleHistoryResult,
    timeZone: TimeZone,
): List<HistoryEntryUi> =
    result.entries
        .sortedWith(
            compareByDescending<
                DynamicScheduleHistoryEntry,
                > { it.capturedAt }
                .thenBy { it.itemId.value },
        )
        .map { it.toHistoryEntryUi(timeZone) }

private fun DynamicScheduleHistoryEntry.toHistoryEntryUi(
    timeZone: TimeZone,
): HistoryEntryUi =
    HistoryEntryUi(
        itemId = itemId.value,

        capturedAtLabel =
            TimeFormatting.dateLabel(capturedAt, timeZone) +
                " " +
                TimeFormatting.timeLabel(capturedAt, timeZone),

        scheduleRevision = scheduleRevision,

        engineVersion = engineVersion,

        hasTrace = hasTrace,
    )
