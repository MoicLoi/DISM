package me.moiclathy.dism.schedule.adaptation.audit

import kotlin.time.Instant

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 14.17
 *
 * 确定性 Audit Builder。
 *
 * 事件 id 全部由 snapshot id 拼出，不使用随机数与时钟，
 * 因此同一份证据永远得到同一条时间线。
 */
class DeterministicDynamicScheduleDecisionAuditBuilder :
    DynamicScheduleDecisionAuditBuilder {

    override fun build(
        snapshot:
            DynamicScheduleDecisionSnapshot,

        trace:
            DynamicScheduleDecisionTraceRecord?,

        generatedAt:
            Instant,

        adaptationPersisted:
            Boolean,

        replayed:
            Boolean,

        verificationStatus:
            String?,

        supersededBy:
            DynamicScheduleDecisionSnapshotId?,
    ):
        DynamicScheduleDecisionAudit {

        val events =
            mutableListOf<DynamicScheduleDecisionAuditEvent>()

        /*
         * ---------------------------------------------------------
         * CREATED
         * ---------------------------------------------------------
         */

        val created =
            DynamicScheduleDecisionAuditEvent(
                id =
                    "created:${snapshot.id}",

                type =
                    DynamicScheduleDecisionAuditEventType.CREATED,

                occurredAt =
                    snapshot.capturedAt,

                subjectId =
                    snapshot.id.toString(),

                message =
                    "Decision was created.",

                metadata =
                    mapOf(
                        "itemId" to
                            snapshot.itemId.toString(),

                        "scheduleRevision" to
                            snapshot.scheduleRevision
                                .toString(),

                        "engineVersion" to
                            snapshot.engineVersion,
                    ),
            )

        events += created

        /*
         * ---------------------------------------------------------
         * SNAPSHOT
         * ---------------------------------------------------------
         */

        val snapshotPersisted =
            DynamicScheduleDecisionAuditEvent(
                id =
                    "snapshot-persisted:${snapshot.id}",

                type =
                    DynamicScheduleDecisionAuditEventType
                        .SNAPSHOT_PERSISTED,

                occurredAt =
                    snapshot.capturedAt,

                subjectId =
                    snapshot.id.toString(),

                parentEventIds =
                    listOf(
                        created.id,
                    ),

                message =
                    "Decision snapshot is available in persistence.",

                metadata =
                    mapOf(
                        "snapshotId" to
                            snapshot.id.toString(),
                    ),
            )

        events += snapshotPersisted

        /*
         * ---------------------------------------------------------
         * TRACE
         * ---------------------------------------------------------
         */

        trace?.let {

            val tracePersisted =
                DynamicScheduleDecisionAuditEvent(
                    id =
                        "trace-persisted:${it.traceId}",

                    type =
                        DynamicScheduleDecisionAuditEventType
                            .TRACE_PERSISTED,

                    occurredAt =
                        it.createdAt,

                    subjectId =
                        it.traceId.toString(),

                    parentEventIds =
                        listOf(
                            snapshotPersisted.id,
                        ),

                    message =
                        "Decision trace is available in persistence.",

                    metadata =
                        mapOf(
                            "traceId" to
                                it.traceId.toString(),

                            "entryCount" to
                                it.trace.entries
                                    .size
                                    .toString(),
                        ),
                )

            events += tracePersisted
        }

        /*
         * ---------------------------------------------------------
         * ADAPTATION
         * ---------------------------------------------------------
         */

        if (adaptationPersisted) {

            val adaptation =
                DynamicScheduleDecisionAuditEvent(
                    id =
                        "adaptation-persisted:${snapshot.itemId}",

                    type =
                        DynamicScheduleDecisionAuditEventType
                            .ADAPTATION_PERSISTED,

                    occurredAt =
                        generatedAt,

                    subjectId =
                        snapshot.itemId.toString(),

                    parentEventIds =
                        listOf(
                            snapshotPersisted.id,
                        ),

                    message =
                        "Adaptive decision state was persisted.",
                )

            events += adaptation
        }

        /*
         * ---------------------------------------------------------
         * REPLAY
         * ---------------------------------------------------------
         */

        if (replayed) {

            val replay =
                DynamicScheduleDecisionAuditEvent(
                    id =
                        "replayed:${snapshot.id}",

                    type =
                        DynamicScheduleDecisionAuditEventType
                            .REPLAYED,

                    occurredAt =
                        generatedAt,

                    subjectId =
                        snapshot.id.toString(),

                    parentEventIds =
                        listOf(
                            snapshotPersisted.id,
                        ),

                    message =
                        "Historical decision was replayed.",
                )

            events += replay
        }

        /*
         * ---------------------------------------------------------
         * VERIFICATION
         * ---------------------------------------------------------
         */

        verificationStatus?.let {

            val verification =
                DynamicScheduleDecisionAuditEvent(
                    id =
                        "verified:${snapshot.id}",

                    type =
                        DynamicScheduleDecisionAuditEventType
                            .VERIFIED,

                    occurredAt =
                        generatedAt,

                    subjectId =
                        snapshot.id.toString(),

                    parentEventIds =
                        listOf(
                            snapshotPersisted.id,
                        ),

                    message =
                        "Historical decision was verified.",

                    metadata =
                        mapOf(
                            "status" to
                                it,
                        ),
                )

            events += verification

            if (
                it == "BEHAVIOR_CHANGED"
            ) {

                events +=
                    DynamicScheduleDecisionAuditEvent(
                        id =
                            "behavior-changed:${snapshot.id}",

                        type =
                            DynamicScheduleDecisionAuditEventType
                                .BEHAVIOR_CHANGED,

                        occurredAt =
                            generatedAt,

                        subjectId =
                            snapshot.id.toString(),

                        parentEventIds =
                            listOf(
                                verification.id,
                            ),

                        message =
                            "Replay verification detected behavior change.",

                        metadata =
                            mapOf(
                                "verificationStatus" to
                                    it,
                            ),
                    )
            }
        }

        /*
         * ---------------------------------------------------------
         * SUPERSEDED（14.17 §九）
         * ---------------------------------------------------------
         *
         * 这是 Audit 真正不可替代的地方：
         *
         * ```text
         * Decision A  priority = 0.50
         *     ↓ 新的 Evidence / Context / Attention Memory
         * Decision B  priority = 0.70
         * ```
         *
         * A 不该被删掉，而应该留下"被 B 取代"这条记录。
         * 否则用户问"为什么昨天这么排、今天变了"，
         * 系统只能回答"不知道"。
         */

        supersededBy?.let {

            events +=
                DynamicScheduleDecisionAuditEvent(
                    id =
                        "superseded:${snapshot.id}",

                    type =
                        DynamicScheduleDecisionAuditEventType
                            .SUPERSEDED,

                    occurredAt =
                        generatedAt,

                    subjectId =
                        snapshot.id.toString(),

                    parentEventIds =
                        listOf(
                            snapshotPersisted.id,
                        ),

                    message =
                        "This decision was superseded by a newer decision.",

                    metadata =
                        mapOf(
                            "supersededBy" to it.toString(),
                        ),
                )
        }

        /*
         * ---------------------------------------------------------
         * STATUS
         * ---------------------------------------------------------
         */

        val status =
            when {
                trace == null ->
                    DynamicScheduleDecisionAuditStatus.INCOMPLETE

                supersededBy != null ->
                    DynamicScheduleDecisionAuditStatus.SUPERSEDED

                else ->
                    DynamicScheduleDecisionAuditStatus.ACTIVE
            }

        val diagnostics =
            buildList {

                if (trace == null) {
                    add(
                        "Decision trace is unavailable.",
                    )
                }

                if (!adaptationPersisted) {
                    add(
                        "Adaptation persistence evidence is unavailable.",
                    )
                }
            }

        return DynamicScheduleDecisionAudit(
            itemId =
                snapshot.itemId,

            snapshotId =
                snapshot.id,

            traceId =
                trace?.traceId,

            createdAt =
                generatedAt,

            status =
                status,

            events =
                events,

            diagnostics =
                diagnostics,
        )
    }
}
