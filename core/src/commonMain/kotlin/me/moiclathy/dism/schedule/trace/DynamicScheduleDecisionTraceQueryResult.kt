package me.moiclathy.dism.schedule.trace

data class DynamicScheduleDecisionTraceQueryResult(
    val records:
        List<DynamicScheduleDecisionTraceRecord>,

    val totalMatched: Int,
)
