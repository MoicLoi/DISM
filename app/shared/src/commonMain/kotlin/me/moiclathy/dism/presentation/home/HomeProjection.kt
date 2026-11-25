package me.moiclathy.dism.presentation.home

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

import me.moiclathy.dism.schedule.api.DynamicScheduleDecisionView
import me.moiclathy.dism.schedule.api.DynamicScheduleEvaluation
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.4：`Evaluation -> HomeUiState` 的**纯映射**。
 *
 * ### 为什么把它和 Presenter 拆开
 *
 * 一次评估（`facade.evaluate`）要跑完整的
 * Dependency → Context → Attention Memory → Adaptation →
 * Attention Projection → Conflict → Explanation 链路。
 *
 * 如果 Today / Schedule / Attention 三个页面各自 `evaluate()` 一次，
 * 同一个模型会被算三遍；更糟的是**三次可能得到不同结果**
 * （如果中途有时间或状态变化），界面上就会出现互相矛盾的三个视图。
 *
 * 所以职责这样切：
 *
 * ```text
 * DismPresenter      评估一次
 *      ↓
 * HomeProjection    纯函数：怎么展示"今天"
 * ScheduleProjection 纯函数：怎么展示全部
 * AttentionProjection 纯函数：怎么展示需要关注的
 * ```
 *
 * 纯函数也意味着这些规则可以直接单测，不需要协程与 Compose。
 */
fun homeUiState(
    model: DynamicScheduleModel,
    evaluation: DynamicScheduleEvaluation,
    now: Instant,
    timeZone: TimeZone,
): HomeUiState =
    HomeUiState(
        dateLabel = homeDateLabel(now, timeZone),
        timeline = homeTimeline(model, evaluation, timeZone),
        attention = homeAttention(model, evaluation),
        isLoading = false,
    )

/**
 * 时间线：**按时间排序**，而不是按优先级。
 *
 * 参考文档里的 Today 页是时间轴，用户读的是"我几点要干什么"。
 * 优先级只影响每条的表现（高亮强弱），不改变顺序——
 * 否则 14:00 的任务会跳到 09:00 的课前面，时间轴就失去意义了。
 *
 * 没有时间的条目（Deadline / Unscheduled）排在最后。
 */
internal fun homeTimeline(
    model: DynamicScheduleModel,
    evaluation: DynamicScheduleEvaluation,
    timeZone: TimeZone,
): List<TimelineEntryUi> {

    val decisionByItem =
        evaluation.decisions.associateBy { it.itemId }

    return model.items
        .filterNot { it.isTerminal }
        .map { item ->
            toTimelineEntry(
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
}

internal fun toTimelineEntry(
    item: DynamicScheduleItem,
    decision: DynamicScheduleDecisionView?,
    timeZone: TimeZone,
): TimelineEntryUi {

    val startAt = item.temporal.startAt

    val endAt = item.temporal.endAt

    return TimelineEntryUi(
        itemId = item.id.value,

        title = item.title,

        timeLabel =
            startAt?.let {
                TimeFormatting.timeLabel(it, timeZone)
            },

        rangeLabel =
            startAt?.let {
                TimeFormatting.rangeLabel(
                    start = it,
                    end = endAt,
                    timeZone = timeZone,
                )
            },

        subtitle = item.externalKey?.takeIf { it.isNotBlank() },

        /*
         * 决策给出的 priority 才是"经过注意力与适配之后"的；
         * 没有决策时退回 model 自己的值。
         */
        priority = decision?.priority ?: item.priority,

        needsAttention =
            decision?.attentionRequired == true,

        explanation = decision?.explanation,
    )
}

internal fun homeDateLabel(
    now: Instant,
    timeZone: TimeZone,
): String =
    TimeFormatting.dateLabel(now, timeZone)

/**
 * Today 页的 Attention 区：只放需要用户**现在**知道的。
 *
 * 按优先级降序、itemId 升序兜底，保证顺序确定。
 */
internal fun homeAttention(
    model: DynamicScheduleModel,
    evaluation: DynamicScheduleEvaluation,
): List<AttentionEntryUi> {

    val titleByItem =
        model.items.associate { it.id to it.title }

    return evaluation.decisions
        .filter { it.attentionRequired == true }
        .sortedWith(
            compareBy(
                { -it.priority },
                { it.itemId.value },
            ),
        )
        .map { decision ->
            AttentionEntryUi(
                itemId = decision.itemId.value,

                title =
                    titleByItem[decision.itemId]
                        ?: decision.title,

                reason =
                    decision.explanation
                        ?: "Needs your attention.",
            )
        }
}
