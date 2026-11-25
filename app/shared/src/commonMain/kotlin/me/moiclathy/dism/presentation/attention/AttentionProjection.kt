package me.moiclathy.dism.presentation.attention

import kotlinx.datetime.TimeZone

import me.moiclathy.dism.presentation.home.TimeFormatting
import me.moiclathy.dism.schedule.api.DynamicScheduleEvaluation
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.6：`Evaluation -> AttentionUiState` 的纯映射。
 *
 * ### 它只回答"系统认为该注意什么"，不重新判断
 *
 * 过滤条件是 `attentionRequired == true`，也就是**决策的输出**。
 *
 * 如果在展示层再写一遍"优先级高于 0.7 就该提醒"，
 * 就会出现两套注意力规则：一套在 Core，一套在 UI，
 * 两者迟早不一致，而且不一致时没人知道该信谁。
 *
 * ### 每一条都尽量带上原因
 *
 * 用户打开 Attention 页通常是因为"它凭什么提醒我"。
 * 没有 [DynamicScheduleDecisionView.explanation] 时这里
 * 如实留 null，不编造理由——见 [AttentionEntryDetailUi.reason]。
 */
fun attentionUiState(
    model: DynamicScheduleModel,
    evaluation: DynamicScheduleEvaluation,
    timeZone: TimeZone,
): AttentionUiState {

    val itemById =
        model.items.associateBy { it.id }

    val entries =
        evaluation.decisions
            .filter { it.attentionRequired == true }
            .sortedWith(
                compareBy(
                    { -it.priority },
                    { it.itemId.value },
                ),
            )
            .map { decision ->
                val item = itemById[decision.itemId]

                val startAt = item?.temporal?.startAt

                AttentionEntryDetailUi(
                    itemId = decision.itemId.value,

                    title = item?.title ?: decision.title,

                    timeLabel =
                        startAt?.let {
                            TimeFormatting.timeLabel(
                                instant = it,
                                timeZone = timeZone,
                            )
                        },

                    priority = decision.priority,

                    reason = decision.explanation,
                )
            }

    return AttentionUiState(
        entries = entries,
        isLoading = false,
    )
}
