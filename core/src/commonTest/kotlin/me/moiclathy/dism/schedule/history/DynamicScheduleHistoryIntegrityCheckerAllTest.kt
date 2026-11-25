package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenance
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceBuilder
import me.moiclathy.dism.schedule.provenance.DynamicScheduleReplayVerificationProvenance
import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 13.17.20
 *
 * 双向历史完整性。
 */
class DynamicScheduleHistoryIntegrityCheckerAllTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val now: Instant =
        Instant.parse("2026-09-26T08:00:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val provenances =
        InMemoryDynamicScheduleDecisionProvenanceStore()

    private val builder =
        DynamicScheduleDecisionProvenanceBuilder()

    private val checker =
        DynamicScheduleHistoryIntegrityChecker(
            snapshotStore = snapshots,
            traceStore = traces,
            replayInputStore = replayInputs,
            provenanceStore = provenances,
        )

    // -----------------------------------------------------------------
    // Test 1：完整历史
    // -----------------------------------------------------------------

    @Test
    fun completeHistoryIsHealthy() = runTest {

        seedCompleteDecision()

        val report = checker.checkAll(checkedAt = now)

        assertTrue(
            report.isHealthy,
            buildString {
                append("完整历史必须健康；issues=")
                append(report.issues.joinToString())
            },
        )

        assertEquals(1, report.checkedSnapshots)

        assertEquals(1, report.checkedReplayInputs)

        assertEquals(1, report.checkedProvenances)

        assertTrue(report.isValid)

        assertEquals(now, report.checkedAt)

        assertEquals(null, report.itemId)
    }

    @Test
    fun emptyDatabaseIsHealthy() = runTest {

        val report = checker.checkAll(checkedAt = now)

        assertTrue(report.isHealthy)

        assertEquals(0, report.checkedSnapshots)
    }

    // -----------------------------------------------------------------
    // Test 2：Snapshot 缺 Trace
    // -----------------------------------------------------------------

    @Test
    fun missingTraceIsDetected() = runTest {

        val snapshot = seedCompleteDecision()

        traces.delete(snapshot.traceId!!)

        val report = checker.checkAll(checkedAt = now)

        assertFalse(report.isHealthy)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .MissingTrace
            },
        )

        assertTrue(report.isDegraded)
    }

    // -----------------------------------------------------------------
    // Test 3：Trace Item 错误
    // -----------------------------------------------------------------

    @Test
    fun traceItemMismatchIsDetected() = runTest {

        val snapshot = seedCompleteDecision()

        traces.save(
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(
                            DynamicScheduleItemId("wrong-item"),
                        )
                        .copy(id = snapshot.traceId!!),

                createdAt = snapshot.capturedAt,

                scheduleRevision = snapshot.scheduleRevision,

                engineVersion = snapshot.engineVersion,
            ),
        )

        val report = checker.checkAll(checkedAt = now)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .TraceItemMismatch
            },
        )

        assertTrue(
            report.isBroken,
            "itemId 不一致属于结构矛盾",
        )
    }

    // -----------------------------------------------------------------
    // Test 4：Provenance 缺 Snapshot
    // -----------------------------------------------------------------

    @Test
    fun provenanceWithoutSnapshotIsDetected() = runTest {

        val snapshot = seedCompleteDecision()

        /*
         * 模拟 Retention 之后的状态：
         * Snapshot 被删掉，Provenance 按设计留下。
         */
        snapshots.delete(snapshot.id)

        val report = checker.checkAll(checkedAt = now)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .ProvenanceMissingSnapshot
            },
        )

        /*
         * 这是"缺"而不是"矛盾"——
         * 否则每次压缩历史都会把数据库判成损坏。
         */
        assertTrue(report.isDegraded)

        assertFalse(report.isBroken)
    }

    // -----------------------------------------------------------------
    // Test 5：Provenance 缺 ReplayInput
    // -----------------------------------------------------------------

    @Test
    fun provenanceWithoutReplayInputIsDetected() = runTest {

        val snapshot = seedCompleteDecision()

        replayInputs.delete(snapshot.id)

        val report = checker.checkAll(checkedAt = now)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .ProvenanceMissingReplayInput
            },
        )
    }

    // -----------------------------------------------------------------
    // Test 6：多个 Provenance —— 只看 latest 会漏
    // -----------------------------------------------------------------

    @Test
    fun brokenEarlierProvenanceIsStillDetected() = runTest {

        val snapshot = seedCompleteDecision()

        /*
         * 再补两条更新的重放记录。
         *
         * 如果完整性检查只看 latestForSnapshot，
         * 就永远看不到最早那条缺 ReplayInput 的记录。
         */
        repeat(2) { index ->
            provenances.save(
                provenanceFor(
                    snapshot = snapshot,
                    replayedAt =
                        now - (index + 1).hours,
                ),
            )
        }

        assertEquals(
            3,
            provenances.queryForSnapshot(snapshot.id).size,
        )

        /*
         * 让**最早**的那条失去 ReplayInput。
         */
        replayInputs.delete(snapshot.id)

        val report = checker.checkAll(checkedAt = now)

        assertEquals(
            3,
            report.checkedProvenances,
        )

        assertEquals(
            3,
            report.issues.count {
                it is DynamicScheduleHistoryIntegrityIssue
                    .ProvenanceMissingReplayInput
            },
            "三条重放记录都应该被检查到",
        )
    }

    // -----------------------------------------------------------------
    // ReplayInput 孤儿
    // -----------------------------------------------------------------

    @Test
    fun orphanReplayInputIsDetectedAndIsSafelyActionable() =
        runTest {

            seedCompleteDecision()

            val orphanId =
                DynamicScheduleDecisionSnapshotId()

            replayInputs.save(
                DynamicScheduleReplayTestData
                    .create()
                    .copy(
                        decisionSnapshot =
                            DynamicScheduleReplayTestData
                                .decisionSnapshot()
                                .copy(id = orphanId),
                    ),
            )

            val report = checker.checkAll(checkedAt = now)

            val orphanIssue =
                assertNotNull(
                    report.issues.firstOrNull {
                        it is DynamicScheduleHistoryIntegrityIssue
                            .ReplayInputMissingSnapshot
                    },
                )

            assertEquals(
                orphanId,
                (
                    orphanIssue as
                        DynamicScheduleHistoryIntegrityIssue
                        .ReplayInputMissingSnapshot
                    ).snapshotId,
            )

            /*
             * 13.17 让 13.9 的孤儿清理动作**第一次真正可达**：
             * per-item 报告里出现了 ReplayInputMissingSnapshot，
             * 于是修复计划可以给出 DeleteOrphanReplayInput。
             */
            val plan =
                DynamicScheduleHistoryRepairPlanner()
                    .plan(checker.check(orphanIssue.itemId))

            val action =
                assertNotNull(
                    plan.actions.firstOrNull {
                        it is DynamicScheduleHistoryRepairAction
                            .DeleteOrphanReplayInput
                    },
                )

            assertEquals(
                orphanId,
                (
                    action as
                        DynamicScheduleHistoryRepairAction
                        .DeleteOrphanReplayInput
                    ).snapshotId,
            )

            assertTrue(
                plan.hasSafeAutomaticAction,
                "孤儿清理是唯一可安全自动执行的动作",
            )
        }

    // -----------------------------------------------------------------
    // Verification 但重放事实不全
    // -----------------------------------------------------------------

    @Test
    fun verificationWithoutCompleteReplayFactsIsDetected() =
        runTest {

            val snapshot = seedCompleteDecision()

            /*
             * 手工构造一条"有校验、但没有行为指纹"的记录，
             * 模拟旧数据库 / 迁移留下的状态。
             */
            provenances.save(
                provenanceFor(
                    snapshot = snapshot,
                    replayedAt = now,
                    behaviorFingerprint = null,
                ),
            )

            val report = checker.checkAll(checkedAt = now)

            assertTrue(
                report.issues.any {
                    it is DynamicScheduleHistoryIntegrityIssue
                        .VerificationWithoutReplay
                },
            )
        }

    @Test
    fun differingVerificationEngineIsNotReportedAsDamage() =
        runTest {

            val snapshot = seedCompleteDecision()

            /*
             * 重放用 12.27、校验用另一个版本：
             * 这是**允许**的（13.17.9），不能报成历史损坏。
             */
            provenances.save(
                provenanceFor(
                    snapshot = snapshot,
                    replayedAt = now,
                    verificationEngineVersion = "99.9",
                ),
            )

            val report = checker.checkAll(checkedAt = now)

            assertTrue(
                report.issues.none {
                    it is DynamicScheduleHistoryIntegrityIssue
                        .VerificationEngineMismatch
                },
                "引擎版本不同不应该被当成完整性错误",
            )
        }

    // -----------------------------------------------------------------
    // 全库报告不能变成 per-item 修复计划
    // -----------------------------------------------------------------

    @Test
    fun wholeDatabaseReportCannotBecomeAPerItemRepairPlan() =
        runTest {

            seedCompleteDecision()

            val report = checker.checkAll(checkedAt = now)

            kotlin.test.assertFailsWith<IllegalArgumentException> {
                DynamicScheduleHistoryRepairPlanner().plan(report)
            }
        }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------


    private suspend fun seedCompleteDecision():
        DynamicScheduleDecisionSnapshot {

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        snapshots.save(snapshot)

        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(decisionSnapshot = snapshot),
        )

        traces.save(
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(itemId)
                        .copy(id = snapshot.traceId!!),

                createdAt = snapshot.capturedAt,

                scheduleRevision = snapshot.scheduleRevision,

                engineVersion = snapshot.engineVersion,
            ),
        )

        provenances.save(
            provenanceFor(
                snapshot = snapshot,
                replayedAt = snapshot.capturedAt,
            ),
        )

        return snapshot
    }

    private fun provenanceFor(
        snapshot: DynamicScheduleDecisionSnapshot,

        replayedAt: Instant,

        behaviorFingerprint: String? = "priority:item-1=0.6",

        verificationEngineVersion: String? = null,
    ): DynamicScheduleDecisionProvenance =
        builder.build(
            snapshot = snapshot,

            historicalEngineVersion =
                snapshot.engineVersion,

            replayEngineVersion =
                snapshot.engineVersion,

            compatibility =
                DynamicScheduleHistoryVersionCompatibility.EXACT,

            migrationId = null,

            evaluatedAt = replayedAt,

            replayedAt = replayedAt,

            behaviorFingerprint = behaviorFingerprint,

            verification =
                DynamicScheduleReplayVerificationProvenance(
                    verifiedAt = replayedAt,

                    status =
                        DynamicScheduleReplayVerificationStatus
                            .EXACT_MATCH,

                    historicalEngineVersion =
                        snapshot.engineVersion,

                    replayEngineVersion =
                        verificationEngineVersion
                            ?: snapshot.engineVersion,

                    differenceCount = 0,
                ),
        )

}
