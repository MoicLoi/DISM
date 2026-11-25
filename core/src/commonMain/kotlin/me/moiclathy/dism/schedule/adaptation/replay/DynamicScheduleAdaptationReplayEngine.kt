package me.moiclathy.dism.schedule.adaptation.replay

import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation

/**
 * 14.12
 *
 * ```text
 * 历史输入 + 历史结果 → 差异
 * ```
 *
 * 实现必须是**纯函数**：同样的输入永远得到同样的结果，
 * 不读时钟、不读数据库。
 */
interface DynamicScheduleAdaptationReplayEngine {

    fun replay(
        input: DynamicScheduleAdaptationReplayInput,
        historical:
            List<DynamicSchedulePriorityAdaptation>,
    ): DynamicScheduleAdaptationReplayResult
}
