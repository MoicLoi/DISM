package me.moiclathy.dism.external.schedule.sync

/**
 * External Schedule Sync Runtime。
 *
 * 负责把 Sync Plan 转换成实际执行结果。
 */
interface ExternalScheduleSyncRuntime {

    fun execute(
        plan: ExternalScheduleSyncPlan,
    ): ExternalScheduleSyncRuntimeResult
}
