package me.moiclathy.dism.schedule.adaptation.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.snapshot
    .DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTrace
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTraceEntry
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace
    .DynamicScheduleDecisionTraceStage

/**
 * 14.17
 *
 * Audit 回答的是"这个决策在生命周期里发生了什么"。
 *
 * 这些测试锁住两点：
 *
 * ```text
 * 没有发生过的事，不能出现在审计里
 * 发生过的事，不能从审计里消失
 * ```
 */
class DeterministicDynamicScheduleDecisionAuditBuilderTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val generatedAt =
        Instant.parse("2026-01-02T10:00:00Z")

    private fun snapshot() =
        DynamicScheduleReplayTestData.decisionSnapshot()

    private fun trace(): DynamicScheduleDecisionTraceRecord =
        DynamicScheduleDecisionTraceRecord(
            trace =
                DynamicScheduleDecisionTrace(
                    itemId = itemId,
                    entries =
                        listOf(
                            DynamicScheduleDecisionTraceEntry(
                                stage =
                                    DynamicScheduleDecisionTraceStage
                                        .INPUT,
                                message = "Input received.",
                            ),
                            DynamicScheduleDecisionTraceEntry(
                                stage =
                                    DynamicScheduleDecisionTraceStage
                                        .OUTPUT,
                                message = "Output produced.",
                                decisive = true,
                            ),
                        ),
                ),
            createdAt = generatedAt,
            scheduleRevision = 1L,
            engineVersion = "test",
        )

    private fun audit(
        trace: DynamicScheduleDecisionTraceRecord? = trace(),
        adaptationPersisted: Boolean = true,
        replayed: Boolean = false,
        verificationStatus: String? = null,
        supersededBy: DynamicScheduleDecisionSnapshotId? = null,
    ) =
        DeterministicDynamicScheduleDecisionAuditBuilder()
            .build(
                snapshot = snapshot(),
                trace = trace,
                generatedAt = generatedAt,
                adaptationPersisted = adaptationPersisted,
                replayed = replayed,
                verificationStatus = verificationStatus,
                supersededBy = supersededBy,
            )

    @Test
    fun createdDecisionProducesAudit() {

        val audit = audit()

        assertEquals(itemId, audit.itemId)

        assertEquals(
            DynamicScheduleDecisionAuditStatus.ACTIVE,
            audit.status,
        )

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.CREATED
            },
        )

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .SNAPSHOT_PERSISTED
            },
        )

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .TRACE_PERSISTED
            },
        )

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .ADAPTATION_PERSISTED
            },
        )

        /*
         * 没重放过、没验证过，就不该有对应事件。
         */
        assertFalse(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.REPLAYED
            },
        )

        assertFalse(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.VERIFIED
            },
        )
    }

    @Test
    fun missingTraceProducesIncompleteAudit() {

        val audit =
            audit(trace = null)

        assertEquals(
            DynamicScheduleDecisionAuditStatus.INCOMPLETE,
            audit.status,
        )

        assertTrue(
            audit.diagnostics.any {
                it.contains("trace")
            },
        )

        assertFalse(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .TRACE_PERSISTED
            },
            "an absent trace must not produce a TRACE_PERSISTED event",
        )

        /*
         * 但 CREATED 仍然必须存在——
         * "证据不全"不等于"什么都没发生"。
         */
        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.CREATED
            },
        )
    }

    @Test
    fun replayProducesReplayAuditEvent() {

        val audit = audit(replayed = true)

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.REPLAYED
            },
        )
    }

    @Test
    fun behaviorChangedProducesAuditEvent() {

        val audit =
            audit(verificationStatus = "BEHAVIOR_CHANGED")

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.VERIFIED
            },
        )

        val changed =
            audit.events.single {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .BEHAVIOR_CHANGED
            }

        assertEquals(
            "BEHAVIOR_CHANGED",
            changed.metadata["verificationStatus"],
        )

        /*
         * BEHAVIOR_CHANGED 必须挂在 VERIFIED 下面，
         * 而不是凭空出现。
         */
        val verified =
            audit.events.single {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.VERIFIED
            }

        assertEquals(
            listOf(verified.id),
            changed.parentEventIds,
        )
    }

    @Test
    fun exactMatchVerificationDoesNotReportBehaviorChange() {

        val audit =
            audit(verificationStatus = "EXACT_MATCH")

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.VERIFIED
            },
        )

        assertFalse(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .BEHAVIOR_CHANGED
            },
        )
    }

    @Test
    fun supersededDecisionKeepsItsHistory() {

        val supersededBy =
            DynamicScheduleDecisionSnapshotId(
                kotlin.uuid.Uuid.parse(
                    "00000000-0000-0000-0000-0000000000aa",
                ),
            )

        val audit = audit(supersededBy = supersededBy)

        assertEquals(
            DynamicScheduleDecisionAuditStatus.SUPERSEDED,
            audit.status,
        )

        val event =
            audit.events.single {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.SUPERSEDED
            }

        assertEquals(
            supersededBy.toString(),
            event.metadata["supersededBy"],
        )

        /*
         * 关键：被取代**不是**删除。
         * 原来的 CREATED / SNAPSHOT 必须都还在。
         */
        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType.CREATED
            },
            "superseding must not erase the original decision",
        )

        assertTrue(
            audit.events.any {
                it.type ==
                    DynamicScheduleDecisionAuditEventType
                        .SNAPSHOT_PERSISTED
            },
        )
    }

    @Test
    fun auditIsDeterministic() {

        /*
         * 必须钉住同一个 snapshot 实例。
         *
         * DynamicScheduleDecisionSnapshotId 默认是 Uuid.random()，
         * 每次调用 decisionSnapshot() 都会得到一个新的 id，
         * 那样比较的就不是"确定性"，而是"两个不同的决策"。
         */
        val snapshot = snapshot()

        /*
         * trace 也要钉住：DynamicScheduleDecisionTraceId 同样是
         * Uuid.random()，两次 trace() 是两个不同的 trace。
         */
        val trace = trace()

        val builder =
            DeterministicDynamicScheduleDecisionAuditBuilder()

        val first =
            builder.build(
                snapshot = snapshot,
                trace = trace,
                generatedAt = generatedAt,
                adaptationPersisted = true,
                replayed = false,
                verificationStatus = null,
                supersededBy = null,
            )

        val second =
            builder.build(
                snapshot = snapshot,
                trace = trace,
                generatedAt = generatedAt,
                adaptationPersisted = true,
                replayed = false,
                verificationStatus = null,
                supersededBy = null,
            )

        assertEquals(first, second)

        assertEquals(
            first.events.map { it.id },
            second.events.map { it.id },
        )
    }

    @Test
    fun eventIdsAreUnique() {

        val audit =
            audit(
                replayed = true,
                verificationStatus = "BEHAVIOR_CHANGED",
            )

        val ids =
            audit.events.map { it.id }

        assertEquals(
            ids.distinct().size,
            ids.size,
        )
    }
}
