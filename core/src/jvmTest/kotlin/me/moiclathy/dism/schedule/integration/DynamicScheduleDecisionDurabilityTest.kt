package me.moiclathy.dism.schedule.integration

import java.io.File

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.ics.IcsScheduleParser
import me.moiclathy.dism.external.schedule.normalization.DeterministicExternalScheduleNormalizer
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseInput
import me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId
import me.moiclathy.dism.persistence.DismPersistenceFactory
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.runtime.DismRuntimeFactory
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBinding
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingId
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingMode
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingSet
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryIntegrityStatus
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRetentionPolicy
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayDeterminismFingerprint
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQuery
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 真实持久化：把 12.x 的 Decision Snapshot / Trace / Replay Input
 * 写进 SQLite，关掉连接再打开，历史决策依然可以确定性重放与校验。
 *
 * 这是"产品化"和"跑通 Demo"之间的分界线：
 *
 * - Vertical Slice（InMemory）证明链路能跑；
 * - 这个测试证明**重启之后**链路还在。
 */
class DynamicScheduleDecisionDurabilityTest {

    private val now =
        Instant.parse("2026-09-24T07:20:00Z")

    private val itemId =
        DynamicScheduleItemId("course-001")

    private val ics =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        UID:course-001
        SUMMARY:实验动物学
        DTSTART:20260924T080000Z
        DTEND:20260924T094000Z
        LOCATION:实验楼201
        X-COURSE-CODE:LAB101
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    @Test
    fun decisionReplayAndVerificationSurviveRestart() = runTest {

        val file = newDatabaseFile("dism-schedule-reopen-")

        try {
            /*
             * ---------------- 第一次运行 ----------------
             */
            val driverA =
                JvmDatabaseDriverFactory(file)
                    .createDriver()

            val databaseA = DismDatabase(driverA)

            val sessionA =
                buildSession(databaseA)

            val snapshotId =
                evaluateOnce(sessionA)

            val replayBefore =
                sessionA.core.replay(snapshotId)

            val fingerprintBefore =
                DynamicScheduleReplayDeterminismFingerprint
                    .from(replayBefore.replayedResult)
                    .canonical

            assertEquals(
                DynamicScheduleReplayVerificationStatus
                    .EXACT_MATCH,
                sessionA.core
                    .verifyReplay(snapshotId)
                    .status,
            )

            /*
             * 真正关闭连接：
             * 之后读到的每一个字节都必须来自磁盘。
             */
            driverA.close()

            /*
             * ---------------- 第二次运行（重启） ----------------
             */
            val driverB =
                JvmDatabaseDriverFactory(file)
                    .createDriver()

            val databaseB = DismDatabase(driverB)

            val sessionB =
                buildSession(databaseB)

            assertEquals(
                snapshotId,
                sessionB.latestSnapshotId(itemId),
                "snapshot 必须从数据库里读回来",
            )

            val replayAfter =
                sessionB.core.replay(snapshotId)

            assertEquals(
                fingerprintBefore,
                DynamicScheduleReplayDeterminismFingerprint
                    .from(replayAfter.replayedResult)
                    .canonical,
                "重放结果必须与重启前完全一致",
            )

            /*
             * 13.15：历史评估时间必须与重启前相同，
             * 而**重放执行时间**是两次不同的真实执行。
             *
             * 旧实现把 replayedAt 设成历史时间，
             * 于是这条断言看起来"很稳"，
             * 实际上是把两个不同的时间混成了一个。
             */
            assertEquals(
                replayBefore.replayInput.evaluatedAt,
                replayAfter.replayInput.evaluatedAt,
                "历史评估时间必须原样从磁盘读回",
            )

            assertEquals(
                now,
                replayAfter.replayInput.evaluatedAt,
            )

            assertNotEquals(
                replayBefore.execution.replayedAt,
                replayAfter.execution.replayedAt,
                "两次重放是两个不同的执行时刻",
            )

            assertEquals(
                DynamicScheduleReplayVerificationStatus
                    .EXACT_MATCH,
                sessionB.core
                    .verifyReplay(snapshotId)
                    .status,
                "重启后 Verification 仍然要判定历史决策可复现",
            )

            /*
             * 历史查询也确实落在磁盘上。
             */
            val history =
                sessionB.history(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemId,
                    ),
                )

            assertEquals(1, history.totalMatched)
            assertEquals(1, history.snapshots.size)

            val traces =
                sessionB.historyTraces(
                    DynamicScheduleDecisionTraceQuery(
                        itemId = itemId,
                    ),
                )

            assertEquals(1, traces.totalMatched)

            assertTrue(
                traces.records
                    .first()
                    .trace
                    .entries
                    .any {
                        it.stage.name == "EXPLANATION"
                    },
                "Trace 的 EXPLANATION 阶段必须一起落盘",
            )

            driverB.close()
        } finally {
            file.delete()
        }
    }

    @Test
    fun dismRuntimeExposesPersistedScheduleDecision() = runTest {

        val file = newDatabaseFile("dism-runtime-schedule-")

        try {
            /*
             * App 层真实入口：
             * DismRuntimeFactory + DismPersistenceFactory。
             */
            val persistenceFactory =
                DismPersistenceFactory(
                    JvmDatabaseDriverFactory(file),
                )

            val runtimeA =
                DismRuntimeFactory(
                    persistenceFactory = persistenceFactory,
                ).create()

            /*
             * 15.3：这个测试需要 session.core（内部重放/校验），
             * 而 runtime.dynamicSchedule() 现在返回的是产品 Facade。
             *
             * 测试本身就在同一模块内，所以直接装配内部 Session——
             * 这正是 15.3 想要的效果：产品路径拿不到 Session，
             * 只有明确知道自己在测内部的地方才拿得到。
             */
            val session =
                DynamicScheduleIntegrationAssembly
                    .from(persistence = persistenceFactory.create())
                    .build(
                        mapOf(itemId to externalKey()),
                    )

            val snapshotId = evaluateOnce(session)

            /*
             * 启动恢复路径：只给 item 列表，
             * 就能把最近一次决策与链路读回来。
             */
            val restored =
                runtimeA.restoreSchedule(listOf(itemId))

            assertEquals(1, restored.snapshots.size)
            assertEquals(1, restored.traces.size)

            val restoredSnapshot =
                assertNotNull(
                    restored.snapshotFor(itemId),
                )

            assertEquals(snapshotId, restoredSnapshot.id)

            /*
             * 恢复出来的就是持久化层那一份，
             * 所以同一个 snapshotId 依然可以校验。
             */
            assertEquals(
                DynamicScheduleReplayVerificationStatus
                    .EXACT_MATCH,
                session.core
                    .verifyReplay(restoredSnapshot.id)
                    .status,
            )

            /*
             * 另一个 Runtime 实例（等价于进程重启）
             * 读同一个数据库文件，依然能恢复。
             */
            val runtimeB =
                DismRuntimeFactory(
                    persistenceFactory =
                        DismPersistenceFactory(
                            JvmDatabaseDriverFactory(file),
                        ),
                ).create()

            val restoredB =
                runtimeB.restoreSchedule(listOf(itemId))

            assertEquals(
                snapshotId,
                assertNotNull(
                    restoredB.snapshotFor(itemId),
                ).id,
            )
        } finally {
            file.delete()
        }
    }

    // -----------------------------------------------------------------
    // 13.5.9 SQLDelight 持久化下的历史压缩
    // -----------------------------------------------------------------

    @Test
    fun historyCompactionWorksOnSqliteAndSurvivesRestart() =
        runTest {

            val file =
                newDatabaseFile("dism-retention-")

            try {
                val driverA =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

                val databaseA = DismDatabase(driverA)

                val sessionA = buildSession(databaseA)

                /*
                 * 三次真实决策，每次时间不同，
                 * 于是同一个 item 有三条历史。
                 */
                val oldest =
                    evaluateOnce(
                        session = sessionA,
                        at = now - 3.hours,
                    )

                evaluateOnce(
                    session = sessionA,
                    at = now - 2.hours,
                )

                val latest =
                    evaluateOnce(
                        session = sessionA,
                        at = now - 1.hours,
                    )

                val before =
                    sessionA.history(
                        DynamicScheduleDecisionSnapshotQuery(
                            itemId = itemId,
                        ),
                    )

                assertEquals(3, before.totalMatched)

                /*
                 * 找出最旧那条的 traceId：
                 * 压缩后它必须连同 Entry 一起消失。
                 */
                val oldestTraceId =
                    before.snapshots
                        .first { it.id == oldest }
                        .traceId

                val compaction =
                    sessionA.compactHistory(
                        itemIds = listOf(itemId),
                        now = now,
                        policy =
                            DynamicScheduleHistoryRetentionPolicy(
                                maxSnapshotsPerItem = 1,
                            ),
                    )

                assertEquals(1, compaction.results.size)

                assertEquals(2, compaction.snapshotsDeleted)

                assertEquals(
                    2,
                    compaction.replayInputsDeleted,
                    "ReplayInput 必须与 Snapshot 同生共死",
                )

                assertEquals(
                    2,
                    compaction.tracesDeleted,
                )

                val after =
                    sessionA.history(
                        DynamicScheduleDecisionSnapshotQuery(
                            itemId = itemId,
                        ),
                    )

                assertEquals(1, after.totalMatched)

                assertEquals(
                    latest,
                    after.snapshots.single().id,
                    "被压缩掉的不能是最新一次决策",
                )

                /*
                 * 孤儿 Entry 检查：
                 * 删掉的 Trace 不能在 entry 表里留下残余。
                 */
                assertTrue(
                    oldestTraceId == null ||
                        databaseA
                            .scheduleDecisionTraceQueries
                            .selectEntriesForTrace(
                                oldestTraceId.value.toString(),
                            )
                            .executeAsList()
                            .isEmpty(),
                    "Trace 删除必须连带删除 Entry",
                )

                /*
                 * 唯一剩下的决策仍然可以确定性重放与校验。
                 */
                assertEquals(
                    DynamicScheduleReplayVerificationStatus
                        .EXACT_MATCH,
                    sessionA.core
                        .verifyReplay(latest)
                        .status,
                )

                driverA.close()

                /*
                 * 重启之后压缩结果必须保持：
                 * 历史不能"复活"，剩的也不能消失。
                 */
                val driverB =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

                val databaseB = DismDatabase(driverB)

                val sessionB = buildSession(databaseB)

                val restored =
                    sessionB.history(
                        DynamicScheduleDecisionSnapshotQuery(
                            itemId = itemId,
                        ),
                    )

                assertEquals(1, restored.totalMatched)

                assertEquals(
                    latest,
                    restored.snapshots.single().id,
                )

                assertEquals(
                    DynamicScheduleReplayVerificationStatus
                        .EXACT_MATCH,
                    sessionB.core
                        .verifyReplay(latest)
                        .status,
                    "压缩后的历史仍然必须可重放、可校验",
                )

                driverB.close()
            } finally {
                file.delete()
            }
        }

    // -----------------------------------------------------------------
    // 13.7.11 完整链路
    // -----------------------------------------------------------------

    @Test
    fun persistedDecisionIsValidAfterRestart() = runTest {

        val file =
            newDatabaseFile("dism-integrity-")

        try {
            val driverA =
                JvmDatabaseDriverFactory(file)
                    .createDriver()

            val sessionA = buildSession(DismDatabase(driverA))

            val snapshotId = evaluateOnce(sessionA)

            /*
             * 刚写完：完整 Decision 必须是 VALID。
             */
            val before =
                sessionA.checkHistoryIntegrity(itemId)

            assertEquals(
                DynamicScheduleHistoryIntegrityStatus.VALID,
                before.status,
                buildString {
                    append("刚写入的历史必须完整；issues=")
                    append(before.issues.joinToString())
                },
            )

            assertTrue(before.issues.isEmpty())

            assertEquals(1, before.checkedSnapshots)

            driverA.close()

            /*
             * 重启后：历史仍然完整，而且仍然可重放、可校验。
             */
            val driverB =
                JvmDatabaseDriverFactory(file)
                    .createDriver()

            val sessionB = buildSession(DismDatabase(driverB))

            val after =
                sessionB.checkHistoryIntegrity(
                    listOf(itemId),
                )

            assertTrue(
                after.isHealthy,
                "重启后历史必须仍然健康",
            )

            assertEquals(1, after.validItems)

            assertEquals(1, after.checkedSnapshots)

            assertEquals(
                DynamicScheduleReplayVerificationStatus
                    .EXACT_MATCH,
                sessionB.core
                    .verifyReplay(snapshotId)
                    .status,
            )

            driverB.close()
        } finally {
            file.delete()
        }
    }

    // -----------------------------------------------------------------
    // 13.12 完整来源链
    // -----------------------------------------------------------------

    @Test
    fun fullDecisionLineageIsRecordedAndSurvivesRestart() =
        runTest {

            val file =
                newDatabaseFile("dism-provenance-")

            try {
                val driverA =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

                val sessionA = buildSession(DismDatabase(driverA))

                val snapshotId = evaluateOnce(sessionA)

                /*
                 * 决策本身还没有被重放过 → 没有来源链记录。
                 */
                assertNull(
                    sessionA.provenanceFor(snapshotId),
                )

                assertEquals(
                    DynamicScheduleReplayVerificationStatus
                        .EXACT_MATCH,
                    sessionA.core
                        .verifyReplay(snapshotId)
                        .status,
                )

                /*
                 * verifyReplay 内含一次 replay，
                 * 因此应该留下了一条来源链，
                 * 并且验证状态已经回填。
                 */
                val provenance =
                    assertNotNull(
                        sessionA.provenanceFor(snapshotId),
                    )

                assertEquals(snapshotId, provenance.snapshotId)

                assertEquals(itemId, provenance.itemId)

                assertEquals(
                    sessionA.engineRegistry.current.version,
                    provenance.historicalEngineVersion,
                )

                assertEquals(
                    sessionA.engineRegistry.current.version,
                    provenance.replayEngineVersion,
                )

                assertEquals(
                    DynamicScheduleHistoryVersionCompatibility
                        .EXACT,
                    provenance.compatibility,
                )

                assertNull(provenance.migrationId)

                assertEquals(
                    DynamicScheduleReplayVerificationStatus
                        .EXACT_MATCH
                        .name,
                    provenance.verificationStatus,
                )

                assertTrue(provenance.verified)

                /*
                 * snapshot → trace 的关联也要在来源链里成立。
                 */
                assertEquals(
                    assertNotNull(
                        sessionA.latestDecision(itemId),
                    ).traceId,
                    provenance.traceId,
                )

                driverA.close()

                /*
                 * 重启之后来源链必须还在——
                 * 它是历史解释索引，不能只活在内存里。
                 */
                val driverB =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

                val sessionB = buildSession(DismDatabase(driverB))

                val restored =
                    assertNotNull(
                        sessionB.provenanceFor(snapshotId),
                    )

                assertEquals(provenance.id, restored.id)

                assertEquals(
                    provenance.verificationStatus,
                    restored.verificationStatus,
                )

                assertEquals(
                    provenance.compatibility,
                    restored.compatibility,
                )

                assertEquals(
                    provenance.historicalEngineVersion,
                    restored.historicalEngineVersion,
                )

                assertEquals(
                    provenance.traceId,
                    restored.traceId,
                )

                /*
                 * item 维度的时间线也读得回来。
                 */
                assertEquals(
                    1,
                    sessionB.provenanceForItem(itemId).size,
                )

                driverB.close()
            } finally {
                file.delete()
            }
        }

    // -----------------------------------------------------------------
    // 13.16 重放来源链落盘
    // -----------------------------------------------------------------

    @Test
    fun replayProvenanceSurvivesRestartWithFingerprintAndVerification() =
        runTest {

            val file =
                newDatabaseFile("dism-replay-provenance-")

            try {
                /*
                 * 两次**不同的**执行：
                 *
                 * 1. 显式重放一次（只有重放，没有校验）
                 * 2. 再校验一次（内部会重放并校验）
                 *
                 * 时间必须不同——它们在现实中就是两次操作，
                 * 也是 provenance 排序能区分"最新一次"的依据。
                 */
                val replayedAt =
                    Instant.parse("2026-09-25T14:30:00Z")

                val verifyReplayAt =
                    Instant.parse("2026-09-25T14:40:00Z")

                val verifiedAt =
                    Instant.parse("2026-09-25T14:41:00Z")

                val driverA =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

                val sessionA = buildSession(DismDatabase(driverA))

                val snapshotId = evaluateOnce(sessionA)

                val replay =
                    sessionA.core.replay(
                        snapshotId = snapshotId,
                        replayedAt = replayedAt,
                    )

                /*
                 * 显式重放留下的那条：没有被校验过。
                 */
                val unverified =
                    assertNotNull(
                        sessionA
                            .provenanceForItem(itemId)
                            .firstOrNull {
                                it.replayedAt == replayedAt
                            },
                    )

                assertNull(
                    unverified.verification,
                    "只重放、没校验的那条不应该有校验事实",
                )

                sessionA.core.verifyReplay(
                    snapshotId = snapshotId,
                    replayedAt = verifyReplayAt,
                    verifiedAt = verifiedAt,
                )

                val before =
                    assertNotNull(
                        sessionA.provenanceFor(snapshotId),
                    )

                assertEquals(verifyReplayAt, before.replayedAt)

                assertEquals(
                    sessionA.engineRegistry.current.version,
                    before.replayEngineVersion,
                )

                assertNotNull(
                    before.behaviorFingerprint,
                    "行为指纹必须落盘",
                )

                val verification =
                    assertNotNull(before.verification)

                assertEquals(
                    DynamicScheduleReplayVerificationStatus
                        .EXACT_MATCH,
                    verification.status,
                )

                assertEquals(verifiedAt, verification.verifiedAt)

                assertEquals(0, verification.differenceCount)

                driverA.close()

                /*
                 * 重启之后必须还能回答：
                 *
                 * 这条历史被重放过吗？
                 * 什么时候？
                 * 用哪个引擎？
                 * 行为指纹是什么？
                 * 校验结果如何？
                 */
                val driverB =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

                val sessionB = buildSession(DismDatabase(driverB))

                val after =
                    assertNotNull(
                        sessionB.provenanceFor(snapshotId),
                    )

                assertEquals(before.id, after.id)

                assertEquals(
                    verifyReplayAt,
                    after.replayedAt,
                )

                assertEquals(
                    before.replayEngineVersion,
                    after.replayEngineVersion,
                )

                assertEquals(
                    before.behaviorFingerprint,
                    after.behaviorFingerprint,
                )

                assertEquals(
                    before.compatibility,
                    after.compatibility,
                )

                assertEquals(before.traceId, after.traceId)

                val restoredVerification =
                    assertNotNull(after.verification)

                assertEquals(
                    verification.status,
                    restoredVerification.status,
                )

                assertEquals(
                    verifiedAt,
                    restoredVerification.verifiedAt,
                )

                assertEquals(
                    verification.differenceCount,
                    restoredVerification.differenceCount,
                )

                assertEquals(
                    verification.replayEngineVersion,
                    restoredVerification.replayEngineVersion,
                )

                /*
                 * 历史时间仍然与重放时间分开。
                 */
                assertNotEquals(
                    after.replayedAt,
                    now,
                    "重放时间不能退化成历史评估时间",
                )

                assertTrue(replay.provenanceId != null)

                driverB.close()
            } finally {
                file.delete()
            }
        }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private fun newDatabaseFile(
        prefix: String,
    ): File {

        val file =
            File.createTempFile(prefix, ".db")

        /*
         * createTempFile 已经建了空文件，
         * 而 JvmDatabaseDriverFactory 用 exists() 判断是否建 Schema，
         * 所以要先删掉。
         */
        file.delete()

        return file
    }

    private fun buildSession(
        database: DismDatabase,
    ): DynamicScheduleSession {

        val snapshots =
            SqlDelightDynamicScheduleDecisionSnapshotStore(
                database,
            )

        return DynamicScheduleIntegrationAssembly(
            snapshotStore = snapshots,

            traceStore =
                SqlDelightDynamicScheduleDecisionTraceStore(
                    database,
                ),

            replayInputStore =
                SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore(
                    database = database,
                    snapshotStore = snapshots,
                ),

            /*
             * 13.12：来源链也必须落盘，
             * 否则"重启后还能解释这条决策"就只剩一半。
             */
            provenanceStore =
                SqlDelightDynamicScheduleDecisionProvenanceStore(
                    database,
                ),
        ).build(
            identityByItem =
                mapOf(itemId to externalKey()),
        )
    }

    private fun externalKey(): String =
        parseSchedule()
            .entries
            .first()
            .id
            .value

    private fun parseSchedule() =
        assertNotNull(
            IcsScheduleParser()
                .parse(
                    ExternalScheduleParseInput(
                        sourceId =
                            ExternalScheduleSourceId(
                                "ics-test",
                            ),

                        rawContent = ics,
                        origin = ExternalScheduleOrigin.ICS,
                    ),
                )
                .schedule,
        )

    /**
     * 跑一次完整决策，返回落盘后的 snapshotId。
     */
    private suspend fun evaluateOnce(
        session: DynamicScheduleSession,

        at: Instant = now,
    ): DynamicScheduleDecisionSnapshotId {

        val schedule = parseSchedule()

        DeterministicExternalScheduleNormalizer()
            .normalize(
                schedule = schedule,
                normalizedAt = at,
            )

        val model =
            session
                .project(
                    schedule = schedule,
                    now = at,
                )
                .projectedModel

        session.evaluate(
            model = model,
            now = at,
            activeContextKeys = setOf("DORM"),
            contextBindings =
                DynamicScheduleContextBindingSet(
                    bindings =
                        listOf(
                            DynamicScheduleContextBinding(
                                id =
                                    DynamicScheduleContextBindingId(
                                        "binding-1",
                                    ),

                                itemId = itemId,

                                contextKey = "DORM",

                                mode =
                                    DynamicScheduleContextBindingMode
                                        .PREFERRED,

                                weight = 0.5,
                            ),
                        ),
                ),
        )

        return assertNotNull(
            session.latestSnapshotId(itemId),
            "evaluate 之后必须能查到 snapshot",
        )
    }
}
