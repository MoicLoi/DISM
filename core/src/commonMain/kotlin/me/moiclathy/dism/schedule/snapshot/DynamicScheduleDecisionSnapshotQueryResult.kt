package me.moiclathy.dism.schedule.snapshot

data class DynamicScheduleDecisionSnapshotQueryResult(
    val snapshots:
        List<DynamicScheduleDecisionSnapshot>,

    val totalMatched: Int,
)
