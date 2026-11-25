package me.moiclathy.dism.presentation.schedule

import kotlinx.datetime.TimeZone

import me.moiclathy.dism.presentation.home.TimeFormatting
import me.moiclathy.dism.presentation.home.toTimelineEntry
import me.moiclathy.dism.schedule.api.DynamicScheduleEvaluation
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleState

/**
 * 16.5：`Evaluation -> ScheduleUiState` 的纯映射。
 *
 * ### 与 Today 最关键的区别：**不过滤终态**
 *
 * Today 是"我现在该做什么"，所以终态条目被隐藏。
 * Schedule 是"我的安排有哪些"，用户会想确认
 * "那件事我确实做完了"。
 *
 * 所以这里保留全部，并用 [ScheduleEntryUi.isTerminal] 标记，
 * 由 UI 决定淡化还是折叠——把"是否显示"做成展示决策，
 * 而不是在这里替用户扔掉数据。
 */
fun scheduleUiState(
    model: DynamicScheduleModel,
    evaluation: DynamicScheduleEvaluation,
    timeZone: TimeZone,
): ScheduleUiState {

    val decisionByItem =
        evaluation.decisions.associateBy { it.itemId }

    /*
     * 复用 Today 的条目映射，再补上 Schedule 需要的两个字段。
     *
     * 刻意共用：时间标签的格式、优先级的取值来源、
     * 副标题的取法只能有一处定义，否则两个页面会慢慢漂移。
     */
    val entries =
        model.items
            .map { item ->
                toScheduleEntry(
                    item = item,
                    decision = decisionByItem[item.id],
                    timeZone = timeZone,
                )
            }
            .sortedWith(
                compareBy(
                    { it.timeLabel == null },
                    { it.timeLabel ?: "" },
                    { -it.priority },
                    { it.itemId },
                ),
            )

    return ScheduleUiState(
        entries = entries,
        isLoading = false,
    )
}

private fun toScheduleEntry(
    item: DynamicScheduleItem,
    decision:
        me.moiclathy.dism.schedule.api
            .DynamicScheduleDecisionView?,
    timeZone: TimeZone,
): ScheduleEntryUi {

    val base =
        toTimelineEntry(
            item = item,
            decision = decision,
            timeZone = timeZone,
        )

    return ScheduleEntryUi(
        itemId = base.itemId,
        title = base.title,
        timeLabel = base.timeLabel,
        rangeLabel = base.rangeLabel,
        subtitle = base.subtitle,
        stateLabel = stateLabel(item.state),
        isTerminal = item.isTerminal,
        priority = base.priority,
        needsAttention = base.needsAttention,
    )
}

/**
 * Core 枚举 → 可读文本。
 *
 * 用 `when` 穷举而不是 `state.name`：新增状态时**编译器会提醒**
 * 这里需要一句人话，而不是让界面上突然冒出 `POSTPONED`
 * 这种全大写标识符。
 */
internal fun stateLabel(
    state: DynamicScheduleState,
): String =
    when (state) {
        DynamicScheduleState.UNSCHEDULED -> "Unscheduled"
        DynamicScheduleState.UPCOMING -> "Upcoming"
        DynamicScheduleState.READY -> "Ready"
        DynamicScheduleState.ACTIVE -> "In progress"
        DynamicScheduleState.BLOCKED -> "Blocked"
        DynamicScheduleState.COMPLETED -> "Completed"
        DynamicScheduleState.CANCELLED -> "Cancelled"
        DynamicScheduleState.EXPIRED -> "Expired"
        DynamicScheduleState.POSTPONED -> "Postponed"
    }
