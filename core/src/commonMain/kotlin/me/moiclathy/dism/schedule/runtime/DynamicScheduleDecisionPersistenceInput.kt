package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

data class DynamicScheduleDecisionPersistenceInput(
    val intelligence:
        DynamicScheduleIntelligenceResult,

    /**
     * 12.26A：
     *
     * Runtime evaluate() 实际接收到的原始 Input。
     *
     * Replay 必须保存这个，而不是从 Result 反推。
     */
    val input:
        DynamicScheduleIntelligenceInput,

    val previous:
        DynamicScheduleIntelligenceRuntimeState,

    val now:
        Instant,

    /**
     * Runtime 当时真正使用的完整 Policy。
     */
    val policy:
        DynamicScheduleIntelligenceRuntimePolicy =
        DynamicScheduleIntelligenceRuntimePolicy(),

    val engineVersion: String = "12.26A",
) {
    init {
        require(engineVersion.isNotBlank())
    }
}
