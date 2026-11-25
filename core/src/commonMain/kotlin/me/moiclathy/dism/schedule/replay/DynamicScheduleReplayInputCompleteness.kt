package me.moiclathy.dism.schedule.replay

/**
 * 描述 Replay 输入是否足够完整。
 */
data class DynamicScheduleReplayInputCompleteness(
    val complete: Boolean,

    /**
     * 缺失的输入。
     */
    val missingInputs: List<String> = emptyList(),

    /**
     * 当前 Replay 可以恢复到什么程度。
     */
    val level: DynamicScheduleReplayCompletenessLevel,
)
