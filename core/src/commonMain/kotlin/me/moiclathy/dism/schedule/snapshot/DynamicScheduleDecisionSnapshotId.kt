package me.moiclathy.dism.schedule.snapshot

import kotlin.uuid.Uuid

data class DynamicScheduleDecisionSnapshotId(
    val value: Uuid = Uuid.random(),
)
