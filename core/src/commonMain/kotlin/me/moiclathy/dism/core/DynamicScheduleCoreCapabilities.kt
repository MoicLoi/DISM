package me.moiclathy.dism.core

/**
 * 13.1
 *
 * Core 当前能力声明。
 *
 * 这是“能力边界”，不是 Runtime 状态。
 */
data class DynamicScheduleCoreCapabilities(
    val dynamicState: Boolean = true,

    val dynamicSemantics: Boolean = true,

    val dynamicWeight: Boolean = true,

    val dependencyResolution: Boolean = true,

    val contextBinding: Boolean = true,

    val attentionMemory: Boolean = true,

    val priorityAdaptation: Boolean = true,

    val attentionProjection: Boolean = true,

    val conflictResolution: Boolean = true,

    val explanation: Boolean = true,

    val decisionTrace: Boolean = true,

    val decisionSnapshot: Boolean = true,

    val historicalReplay: Boolean = true,

    val replayVerification: Boolean = true,
) {

    companion object {

        fun current():
            DynamicScheduleCoreCapabilities =
            DynamicScheduleCoreCapabilities()
    }
}
