package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation

/**
 * 14.7
 *
 * 批量合并历史 Priority Adaptation 与当前 Context Influence。
 *
 * 缺少 Context Influence 的 item 按 `contextFactor = 1.0` 处理，
 * 保证输出与输入 adaptations 一一对应。
 */
class DynamicScheduleContextSensitivePriorityService {

    fun combine(
        adaptations:
            List<DynamicSchedulePriorityAdaptation>,

        contextInfluences:
            List<DynamicScheduleContextPriorityInfluence>,
    ): List<DynamicScheduleContextSensitivePriority> {

        val contextByItem =
            contextInfluences.associateBy {
                it.itemId
            }

        return adaptations
            .sortedBy {
                it.itemId.value
            }
            .map { adaptation ->

                val context =
                    contextByItem[
                        adaptation.itemId
                    ]
                        ?: DynamicScheduleContextPriorityInfluence(
                            itemId =
                                adaptation.itemId,

                            contextFactor = 1.0,

                            contextMatch = 0.0,

                            excluded = false,

                            requiredSatisfied = true,

                            matchedContextKeys =
                                emptySet(),

                            reasons =
                                listOf(
                                    "No context influence",
                                ),
                        )

                DynamicScheduleContextSensitivePriority.from(
                    adaptation = adaptation,
                    context = context,
                )
            }
    }
}
