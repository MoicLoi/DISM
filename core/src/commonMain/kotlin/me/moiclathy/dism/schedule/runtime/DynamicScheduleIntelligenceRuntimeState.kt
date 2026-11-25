package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.attention.runtime.AttentionRuntimeState
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionHistory
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionKey
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleIntelligenceRuntimeState(
    val attentionRuntimeState:
        AttentionRuntimeState =
        AttentionRuntimeState(),

    /**
     * Schedule-facing attention history.
     *
     * key = DynamicScheduleItemId.value
     */
    val scheduleHistories:
        Map<String, DynamicScheduleAttentionHistory> =
        emptyMap(),

    /**
     * 12.11：Context-aware attention history。
     *
     * 与 scheduleHistories 是两个不同层次的记忆：
     *
     * scheduleHistories
     *     = “这个东西通常值不值得提醒？”
     *
     * contextAttentionHistories
     *     = “这个东西在这个 Context 下值不值得提醒？”
     */
    val contextAttentionHistories:
        Map<
            DynamicScheduleContextAttentionKey,
            DynamicScheduleContextAttentionHistory,
            > =
        emptyMap(),
)
