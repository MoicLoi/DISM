package me.moiclathy.dism.schedule.trace

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleDecisionTraceRecord(
    val trace: DynamicScheduleDecisionTrace,

    val createdAt: Instant,

    /**
     * 产生这次决策时所使用的 Schedule revision。
     */
    val scheduleRevision: Long,

    /**
     * 以后规则引擎升级时，
     * 可以知道某次历史决策由哪个版本产生。
     */
    val engineVersion: String = "12.15",
) {
    init {
        require(scheduleRevision >= 0)
        require(engineVersion.isNotBlank())
    }

    val traceId: DynamicScheduleDecisionTraceId
        get() = trace.id

    val itemId: DynamicScheduleItemId
        get() = trace.itemId
}
