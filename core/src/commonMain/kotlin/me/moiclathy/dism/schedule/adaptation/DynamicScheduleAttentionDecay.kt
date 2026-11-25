package me.moiclathy.dism.schedule.adaptation

/**
 * 12.9 boundary。
 *
 * 使用普通 interface：Kotlin 不允许 fun interface 的
 * 唯一抽象方法带默认参数值（`policy` 需要默认值）。
 */
interface DynamicScheduleAttentionDecay {

    fun decay(
        input: DynamicScheduleAttentionDecayInput,
        policy: DynamicScheduleAttentionDecayPolicy =
            DynamicScheduleAttentionDecayPolicy(),
    ): DynamicScheduleAttentionDecayResult
}
