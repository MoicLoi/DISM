package me.moiclathy.dism.schedule.attention

/**
 * 12.6 boundary。
 *
 * 这里使用普通 interface 而不是 fun interface：
 * Kotlin 不允许 fun interface 的唯一抽象方法带默认参数值，
 * 而 `policy` 需要默认值（否则调用方必须显式传入）。
 */
interface DynamicScheduleAttentionProjector {

    fun project(
        input: DynamicScheduleAttentionInput,
        policy: DynamicScheduleAttentionPolicy =
            DynamicScheduleAttentionPolicy(),
    ): DynamicScheduleAttentionProjectionResult
}
