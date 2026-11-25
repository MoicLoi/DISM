package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * Time-flow Runtime。
 */
interface EventTimeRuntime {

    /**
     * 启动 Runtime。
     */
    fun start(
        evaluatedAt: Instant,
    ): EventTimeRuntimeEvaluation

    /**
     * 暂停 Runtime。
     */
    fun pause(
        evaluatedAt: Instant,
    ): EventTimeRuntimeEvaluation

    /**
     * 停止 Runtime。
     */
    fun stop(
        evaluatedAt: Instant,
    ): EventTimeRuntimeEvaluation

    /**
     * 执行一次时间流 Tick。
     */
    fun tick(
        input: EventTimeRuntimeInput,
    ): EventTimeRuntimeEvaluation

    /**
     * 获取当前 Runtime 状态。
     */
    fun state(): EventTimeRuntimeState
}
