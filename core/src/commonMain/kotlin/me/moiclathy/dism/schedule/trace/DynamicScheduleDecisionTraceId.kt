package me.moiclathy.dism.schedule.trace

import kotlin.uuid.Uuid

data class DynamicScheduleDecisionTraceId(
    val value: Uuid = Uuid.random(),
)
