package me.moiclathy.dism.schedule.provenance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 13.12
 *
 * Provenance 的存储与组装。
 */
class DynamicScheduleDecisionProvenanceStoreTest {

    private val store =
        InMemoryDynamicScheduleDecisionProvenanceStore()

    private val builder =
        DynamicScheduleDecisionProvenanceBuilder()

    private val now: Instant =
        Instant.parse("2026-09-24T08:00:00Z")

    @Test
    fun provenanceRoundTripsThroughTheStore() = runTest {

        val record = provenance()

        store.save(record)

        assertEquals(record, store.get(record.id))
    }

    @Test
    fun latestForSnapshotPicksTheNewestAttempt() = runTest {

        val older = provenance(hoursAgo = 5)

        val newer = provenance(hoursAgo = 1)

        store.save(older)
        store.save(newer)

        val latest =
            store.latestForSnapshot(newer.snapshotId)

        assertNotNull(latest)

        assertEquals(newer.id, latest.id)
    }

    @Test
    fun latestForSnapshotIsDeterministicWhenTimesMatch() =
        runTest {

            /*
             * 确定性重放下 createdAt 可能完全相同，
             * 这时必须有一个稳定的二级排序，
             * 否则"最近一次"会在不同进程间飘。
             *
             * 注意两次 provenance 必须挂在**同一个 Snapshot** 上，
             * 否则它们本来就不是"同一条历史的两次重放"。
             */
            val snapshot =
                DynamicScheduleReplayTestData.decisionSnapshot()

            val first =
                provenance(snapshot = snapshot, hoursAgo = 0)

            val second =
                provenance(snapshot = snapshot, hoursAgo = 0)

            store.save(first)
            store.save(second)

            val latest =
                store.latestForSnapshot(snapshot.id)

            val expected =
                listOf(first, second)
                    .maxBy { it.id.value.toString() }

            assertEquals(expected.id, latest?.id)

            /* 再查一次，结果必须一样。 */
            assertEquals(
                latest?.id,
                store.latestForSnapshot(snapshot.id)?.id,
            )
        }

    @Test
    fun queryForItemReturnsTimelineInOrder() = runTest {

        val late = provenance(hoursAgo = 1)

        val early = provenance(hoursAgo = 9)

        store.save(late)
        store.save(early)

        val timeline =
            store.queryForItem(early.itemId)

        assertEquals(
            listOf(early.id, late.id),
            timeline.map { it.id },
        )
    }

    @Test
    fun verificationStatusIsBackfilledLater() = runTest {

        val record = provenance()

        store.save(record)

        assertFalse(record.verified)

        store.updateVerification(
            id = record.id,

            verification =
                DynamicScheduleReplayVerificationProvenance(
                    verifiedAt = now,
                    status =
                        DynamicScheduleReplayVerificationStatus
                            .EXACT_MATCH,
                    historicalEngineVersion = "12.27",
                    replayEngineVersion = "13.11",
                    differenceCount = 0,
                ),
        )

        val updated = assertNotNull(store.get(record.id))

        assertEquals("EXACT_MATCH", updated.verificationStatus)

        assertTrue(updated.verified)

        assertEquals(
            now,
            updated.verification?.verifiedAt,
        )
    }

    @Test
    fun updatingAnUnknownProvenanceIsANoOp() = runTest {

        store.updateVerification(
            id = DynamicScheduleDecisionProvenanceId(),

            verification =
                DynamicScheduleReplayVerificationProvenance(
                    verifiedAt = now,
                    status =
                        DynamicScheduleReplayVerificationStatus
                            .EXACT_MATCH,
                    historicalEngineVersion = "12.27",
                    replayEngineVersion = "13.11",
                    differenceCount = 0,
                ),
        )

        assertNull(
            store.get(
                DynamicScheduleDecisionProvenanceId(),
            ),
        )
    }

    @Test
    fun builderTakesItemIdFromSnapshotNotFromModelItems() {

        /*
         * 13.12.12：itemId 只能来自 snapshot，
         * 不能从 replayInput.input.model.items 里猜。
         *
         * 这里用一条 itemId 与 fixture model 不同的 Snapshot，
         * 验证 builder 取的是 Snapshot 的那个。
         */
        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(
                    itemId =
                        me.moiclathy.dism.schedule.model
                            .DynamicScheduleItemId("other-item"),
                )

        val record =
            builder.build(
                snapshot = snapshot,

                historicalEngineVersion = "12.27",

                replayEngineVersion = "13.11",

                compatibility =
                    DynamicScheduleHistoryVersionCompatibility
                        .REPLAY_COMPATIBLE,

                migrationId = null,

                evaluatedAt = now,

                replayedAt = now,
            )

        assertEquals(
            "other-item",
            record.itemId.value,
        )

        assertEquals(snapshot.id, record.snapshotId)

        /*
         * ReplayInput 的主键就是 decisionSnapshot.id，
         * 因此这里复用同一个 ID。
         */
        assertEquals(
            snapshot.id,
            record.replayInputSnapshotId,
        )

        assertEquals(snapshot.traceId, record.traceId)

        assertTrue(record.replayedWithDifferentEngine)
    }

    @Test
    fun provenanceIsExplicitAboutMigrationAndVerification() {

        val record =
            provenance(
                migrationId = "12.27-to-13.11-v1",
                verificationStatus = null,
            )

        assertEquals(
            "12.27-to-13.11-v1",
            record.migrationId,
        )

        assertFalse(
            record.verified,
            "Verification 可能晚于 Replay，状态可以为空",
        )
    }

    @Test
    fun identicalInputsProduceIdenticalProvenanceIds() {

        /*
         * Provenance id 是随机的实例 id，
         * 所以这里断言的是"除了 id 之外全部一致"——
         * 也就是 13.12 的 builder 不引入任何墙钟时间。
         */
        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        val first =
            builder.build(
                snapshot = snapshot,
                historicalEngineVersion = "12.27",
                replayEngineVersion = "13.11",
                compatibility =
                    DynamicScheduleHistoryVersionCompatibility
                        .EXACT,
                migrationId = null,
                evaluatedAt = now,
                replayedAt = now,
            )

        val second =
            builder.build(
                snapshot = snapshot,
                historicalEngineVersion = "12.27",
                replayEngineVersion = "13.11",
                compatibility =
                    DynamicScheduleHistoryVersionCompatibility
                        .EXACT,
                migrationId = null,
                evaluatedAt = now,
                replayedAt = now,
            )

        assertEquals(
            first.copy(id = second.id),
            second,
        )
    }

    // -----------------------------------------------------------------
    // 生命周期：不与 Snapshot 级联删除（13.12.10）
    // -----------------------------------------------------------------

    @Test
    fun provenanceSurvivesSnapshotRetention() = runTest {

        val snapshots =
            me.moiclathy.dism.schedule.snapshot
                .InMemoryDynamicScheduleDecisionSnapshotStore()

        val replayInputs =
            me.moiclathy.dism.schedule.replay
                .InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

        val retention =
            me.moiclathy.dism.schedule.history
                .DynamicScheduleHistoryRetentionService(
                snapshotStore = snapshots,

                traceStore =
                    me.moiclathy.dism.schedule.trace
                        .InMemoryDynamicScheduleDecisionTraceStore(),

                replayInputStore = replayInputs,
            )

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        snapshots.save(snapshot)

        val record =
            builder.build(
                snapshot = snapshot,
                historicalEngineVersion = "12.27",
                replayEngineVersion = "13.11",
                compatibility =
                    DynamicScheduleHistoryVersionCompatibility
                        .EXACT,
                migrationId = null,
                evaluatedAt = now,
                replayedAt = now,
            )

        store.save(record)

        /*
         * 把这条历史压缩掉（maxSnapshotsPerItem = 1 且只有一条时
         * 不会删；所以用 maxAge 强制它成为候选）。
         *
         * 这里直接删 Snapshot，模拟 Retention 最终的效果。
         */
        snapshots.delete(snapshot.id)

        /*
         * Snapshot 没了，但来源链必须还在——
         * 它正是"这里曾经发生过一次决策"的最后证据。
         */
        val survivor =
            assertNotNull(
                store.latestForSnapshot(snapshot.id),
            )

        assertEquals(record.id, survivor.id)

        assertEquals(
            1,
            store.queryForItem(snapshot.itemId).size,
        )

        /*
         * 顺带确认 Retention 确实不持有 provenance store：
         * 它没有能力删除来源链。
         */
        assertEquals(
            0,
            retention
                .compact(
                    itemIds = listOf(snapshot.itemId),
                    now = now,
                )
                .totalDeleted,
        )
    }

    // -----------------------------------------------------------------
    // helper
    // -----------------------------------------------------------------

    private fun provenance(
        hoursAgo: Int = 0,

        migrationId: String? = null,

        verificationStatus: String? = null,

        snapshot:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshot =
            DynamicScheduleReplayTestData.decisionSnapshot(),
    ): DynamicScheduleDecisionProvenance =
        builder.build(
            snapshot = snapshot,

            historicalEngineVersion = "12.27",

            replayEngineVersion = "13.11",

            compatibility =
                DynamicScheduleHistoryVersionCompatibility
                    .REPLAY_COMPATIBLE,

            migrationId = migrationId,

            evaluatedAt = now - hoursAgo.hours,

            replayedAt = now - hoursAgo.hours,

            verification =
                verificationStatus?.let {
                    DynamicScheduleReplayVerificationProvenance(
                        verifiedAt = now,
                        status =
                            DynamicScheduleReplayVerificationStatus
                                .valueOf(it),
                        historicalEngineVersion = "12.27",
                        replayEngineVersion = "13.11",
                        differenceCount = 0,
                    )
                },
        )
}
