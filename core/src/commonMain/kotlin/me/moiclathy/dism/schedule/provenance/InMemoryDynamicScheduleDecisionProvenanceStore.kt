package me.moiclathy.dism.schedule.provenance

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 13.12.7
 */
class InMemoryDynamicScheduleDecisionProvenanceStore :
    DynamicScheduleDecisionProvenanceStore {

    private val records =
        LinkedHashMap<
            DynamicScheduleDecisionProvenanceId,
            DynamicScheduleDecisionProvenance,
            >()

    override suspend fun save(
        provenance:
            DynamicScheduleDecisionProvenance,
    ) {
        records[provenance.id] = provenance
    }

    override suspend fun get(
        id:
            DynamicScheduleDecisionProvenanceId,
    ): DynamicScheduleDecisionProvenance? =
        records[id]

    override suspend fun latestForSnapshot(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionProvenance? =
        records.values
            .filter {
                it.snapshotId == snapshotId
            }
            .maxWithOrNull(
                compareBy<
                    DynamicScheduleDecisionProvenance,
                    > {
                    it.createdAt
                }.thenBy {
                    it.id.value.toString()
                },
            )

    override suspend fun queryForSnapshot(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): List<DynamicScheduleDecisionProvenance> =
        records.values
            .filter {
                it.snapshotId == snapshotId
            }
            .sortedWith(
                compareByDescending<
                    DynamicScheduleDecisionProvenance,
                    > {
                    it.replayedAt ?: it.createdAt
                }.thenByDescending {
                    it.id.value.toString()
                },
            )

    override suspend fun all():
        List<DynamicScheduleDecisionProvenance> =
        records.values.toList()

    override suspend fun queryForItem(
        itemId:
            DynamicScheduleItemId,
    ): List<DynamicScheduleDecisionProvenance> =
        records.values
            .filter {
                it.itemId == itemId
            }
            .sortedWith(
                compareBy<
                    DynamicScheduleDecisionProvenance,
                    > {
                    it.createdAt
                }.thenBy {
                    it.id.value.toString()
                },
            )

    override suspend fun updateVerification(
        id:
            DynamicScheduleDecisionProvenanceId,

        verification:
            DynamicScheduleReplayVerificationProvenance,
    ) {
        val existing = records[id] ?: return

        records[id] =
            existing.copy(verification = verification)
    }

    override suspend fun clear() {
        records.clear()
    }
}
