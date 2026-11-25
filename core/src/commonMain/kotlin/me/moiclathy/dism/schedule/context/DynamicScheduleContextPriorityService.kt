package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 14.7
 *
 * 对整个 Schedule Model 计算 Context Priority Influence。
 *
 * 没有对应 `DynamicScheduleContextMatch` 的 item 会得到
 * `contextFactor = 1.0`（不影响），而不是被跳过——
 * 结果必须与 `model.items` 一一对应，调用方才不用自己补齐。
 */
class DynamicScheduleContextPriorityService(
    private val adapter:
        DynamicScheduleContextPriorityAdapter =
        DeterministicDynamicScheduleContextPriorityAdapter(),
) {

    fun adapt(
        model: DynamicScheduleModel,
        matches: List<DynamicScheduleContextMatch>,
        policy: DynamicScheduleContextPriorityPolicy =
            DynamicScheduleContextPriorityPolicy(),
    ): List<DynamicScheduleContextPriorityInfluence> {

        val matchByItem =
            matches.associateBy { it.itemId }

        return model.items
            .sortedBy { it.id.value }
            .map { item ->

                val match =
                    matchByItem[item.id]

                if (match == null) {

                    DynamicScheduleContextPriorityInfluence(
                        itemId = item.id,
                        contextFactor = 1.0,
                        contextMatch = 0.0,
                        excluded = false,
                        requiredSatisfied = true,
                        matchedContextKeys =
                            emptySet(),
                        reasons =
                            listOf(
                                "No context match; priority unchanged",
                            ),
                    )

                } else {

                    adapter.adapt(
                        match = match,
                        policy = policy,
                    )
                }
            }
    }
}
