package me.moiclathy.dism.schedule.replay

import kotlin.time.Instant

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot

/**
 * 12.26A
 *
 * 一次 Dynamic Schedule Decision 的完整历史 Runtime Input。
 *
 * 注意：
 *
 * DynamicScheduleDecisionSnapshot
 *      = 决策结果/摘要
 *
 * DynamicScheduleDecisionReplayInputSnapshot
 *      = 当时 Runtime 真正使用的输入
 *
 * 后者才是 12.27 True Historical Replay 的基础。
 */
data class DynamicScheduleDecisionReplayInputSnapshot(

    /**
     * 与本次 Replay Input 对应的 Decision Snapshot。
     *
     * 通过 snapshot.id 将：
     *
     * Snapshot
     *      ↕
     * Replay Input
     *
     * 进行稳定关联。
     */
    val decisionSnapshot:
        DynamicScheduleDecisionSnapshot,

    /**
     * 当时 Runtime 接收到的完整 Schedule Intelligence Input。
     *
     * 内含：
     *
     * - DynamicScheduleModel
     * - DynamicScheduleDependencySet
     * - DynamicScheduleContextBindingSet
     * - activeContextKeys
     */
    val input:
        DynamicScheduleIntelligenceInput,

    /**
     * Runtime 执行前的完整历史状态。
     *
     * 这一项非常重要。
     *
     * AttentionRuntimeState、
     * Schedule Attention History、
     * Context Attention History
     * 都可能影响下一次决策。
     */
    val previousState:
        DynamicScheduleIntelligenceRuntimeState,

    /**
     * 当时 Runtime 使用的完整 Policy。
     *
     * 不再只保存 DynamicSchedulePolicySnapshot
     * 那种摘要，而是保存真正参与 Runtime.evaluate()
     * 的 policy。
     */
    val policy:
        DynamicScheduleIntelligenceRuntimePolicy,

    /**
     * 当时 Runtime 执行的时间。
     *
     * Replay 时禁止用 Clock.System.now() 替代。
     */
    val evaluatedAt:
        Instant,

    /**
     * 当时 Schedule 的 revision。
     */
    val scheduleRevision:
        Long,

    /**
     * 当时执行该决策的 engine version。
     */
    val engineVersion:
        String,
) {

    init {
        require(scheduleRevision >= 0) {
            "scheduleRevision must be >= 0"
        }

        require(engineVersion.isNotBlank()) {
            "engineVersion must not be blank"
        }
    }

    /**
     * 方便 Replay Service 获取当时的 Schedule Model。
     */
    val scheduleModel
        get() =
            input.model

    /**
     * 方便 Replay Service 获取当时 Context。
     */
    val activeContextKeys
        get() =
            input.activeContextKeys
}
