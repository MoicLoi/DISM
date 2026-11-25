package me.moiclathy.dism.event.runtime

/**
 * Time-flow Runtime Recovery。
 *
 * 负责从已经持久化的 Runtime 历史中恢复运行时状态。
 */
interface EventTimeRuntimeRecovery {

    /**
     * 从 Persistence 中恢复 Runtime。
     */
    fun recover():
        EventTimeRuntimeRecoveryResult
}
