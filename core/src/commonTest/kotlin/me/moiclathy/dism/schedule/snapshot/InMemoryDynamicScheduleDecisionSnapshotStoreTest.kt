package me.moiclathy.dism.schedule.snapshot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class InMemoryDynamicScheduleDecisionSnapshotStoreTest {

    private val store =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private fun createSnapshot(
        itemId: String,
        revision: Long,
        capturedAt: String,
        engineVersion: String = "12.18",
    ): DynamicScheduleDecisionSnapshot {

        return DynamicScheduleDecisionSnapshot(
            itemId =
                DynamicScheduleItemId(itemId),

            capturedAt =
                Instant.parse(capturedAt),

            scheduleRevision =
                revision,

            engineVersion =
                engineVersion,

            item =
                DynamicScheduleItemSnapshot(
                    itemId =
                        DynamicScheduleItemId(itemId),

                    title = "实验课",

                    state =
                        DynamicScheduleState.READY,

                    priority = 0.8,

                    confidence = 1.0,
                ),

            context =
                DynamicScheduleContextSnapshot(
                    activeContextKeys =
                        setOf("CLASSROOM"),
                ),

            attentionMemory =
                DynamicScheduleAttentionMemorySnapshot(
                    globalFactor = 0.8,
                    contextFactor = 1.0,
                    adaptationFactor = 1.0,
                ),

            dependency =
                DynamicScheduleDependencySnapshot(
                    requiredSatisfied = true,
                ),

            policy =
                DynamicSchedulePolicySnapshot(
                    minimumAttentionPriority = 0.25,
                    minimumAttentionConfidence = 0.25,
                    minimumHardConstraintPriority = 0.75,
                    explicitRequestPriority = 1.0,
                    maximumLearnedSuppression = 0.5,
                ),
        )
    }

    @Test
    fun saveAndGetWorks() = runTest {

        val snapshot =
            createSnapshot(
                itemId = "item-1",
                revision = 10,
                capturedAt =
                    "2026-09-23T08:00:00Z",
            )

        store.save(snapshot)

        val result =
            store.get(snapshot.id)

        assertNotNull(result)

        assertEquals(
            snapshot.id,
            result.id,
        )

        assertEquals(
            10,
            result.scheduleRevision,
        )
    }

    @Test
    fun latestForItemReturnsNewest() = runTest {

        val older =
            createSnapshot(
                itemId = "item-1",
                revision = 10,
                capturedAt =
                    "2026-09-23T08:00:00Z",
            )

        val newer =
            createSnapshot(
                itemId = "item-1",
                revision = 11,
                capturedAt =
                    "2026-09-23T09:00:00Z",
            )

        store.save(older)
        store.save(newer)

        val result =
            store.findLatestByItemId(
                DynamicScheduleItemId("item-1"),
            )

        assertNotNull(result)

        assertEquals(
            newer.id,
            result.id,
        )
    }

    @Test
    fun queryByRevisionWorks() = runTest {

        store.save(
            createSnapshot(
                itemId = "item-1",
                revision = 10,
                capturedAt =
                    "2026-09-23T08:00:00Z",
            ),
        )

        store.save(
            createSnapshot(
                itemId = "item-1",
                revision = 11,
                capturedAt =
                    "2026-09-23T09:00:00Z",
            ),
        )

        val result =
            store.query(
                DynamicScheduleDecisionSnapshotQuery(
                    scheduleRevision = 11,
                ),
            )

        assertEquals(
            1,
            result.totalMatched,
        )

        assertEquals(
            11,
            result.snapshots
                .single()
                .scheduleRevision,
        )
    }

    @Test
    fun queryByEngineVersionWorks() = runTest {

        store.save(
            createSnapshot(
                itemId = "item-1",
                revision = 10,
                capturedAt =
                    "2026-09-23T08:00:00Z",
                engineVersion = "12.17",
            ),
        )

        store.save(
            createSnapshot(
                itemId = "item-1",
                revision = 11,
                capturedAt =
                    "2026-09-23T09:00:00Z",
                engineVersion = "12.18",
            ),
        )

        val result =
            store.query(
                DynamicScheduleDecisionSnapshotQuery(
                    engineVersion = "12.18",
                ),
            )

        assertEquals(
            1,
            result.totalMatched,
        )

        assertTrue(
            result.snapshots
                .single()
                .engineVersion == "12.18",
        )
    }
}
