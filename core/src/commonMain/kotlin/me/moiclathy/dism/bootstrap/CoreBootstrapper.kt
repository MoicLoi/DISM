package me.moiclathy.dism.bootstrap

import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 启动恢复：把长期状态从 Persistence 读回可用的 Domain 对象。
 *
 * 世界状态（同步、严格）：
 *
 * ```text
 * Entity → Evidence → RelationState → TransitionLog
 * ```
 *
 * 调度历史（suspend、容错）：
 *
 * ```text
 * latest Snapshot
 *       │
 *       ▼ Snapshot.traceId
 *     exact Trace
 * ```
 *
 * 13.4 的两条原则：
 *
 * 1. **只读最近一行**——决策历史会随时间无限增长，
 *    启动路径上不能把它整体读进内存；
 *    历史查询走 `DynamicScheduleSession.history()`。
 *
 * 2. **历史损坏不阻塞启动**——任何一条历史读不出来，
 *    都记成 [ScheduleRestoreDiagnostic] 并跳过，
 *    而不是让 `DismRuntime` 创建失败。
 *
 * 世界状态不做这种容错：它损坏时确实应该更严格处理。
 */
class CoreBootstrapper(
    private val persistence: DismPersistence,
) {

    fun restore(): RestoredCoreState {

        val entities =
            persistence.entities.all()

        val evidence =
            persistence.evidence.all()

        val relationStates =
            persistence.relationStates.loadAll()

        val transitions =
            persistence.transitionLog.all()

        return RestoredCoreState(
            entities = entities,
            evidence = evidence,
            relationStates = relationStates,
            transitions = transitions,
        )
    }

    /**
     * 恢复指定 item 的最近一次调度决策。
     *
     * Trace 通过 `Snapshot.traceId` 精确恢复，
     * 从而保证：
     *
     * ```text
     * snapshotFor(item).traceId == traceFor(item).id
     * ```
     *
     * 永远成立——两者指向同一次决策。
     */
    suspend fun restoreSchedule(
        itemIds: List<DynamicScheduleItemId>,
    ): RestoredScheduleState {

        val snapshots =
            mutableListOf<DynamicScheduleDecisionSnapshot>()

        val traces =
            mutableListOf<DynamicScheduleDecisionTraceRecord>()

        val diagnostics =
            mutableListOf<ScheduleRestoreDiagnostic>()

        itemIds
            .distinct()
            .forEach { itemId ->

                val snapshot =
                    runCatching {
                        persistence
                            .scheduleSnapshots
                            .findLatestByItemId(itemId)
                    }
                        .getOrElse { failure ->

                            diagnostics +=
                                ScheduleRestoreDiagnostic
                                    .SnapshotUnreadable(
                                        itemId = itemId,

                                        reason =
                                            failure.describe(),
                                    )

                            null
                        }

                if (snapshot == null) {

                    /*
                     * 读取出错时已经记过诊断，
                     * 这里只处理"确实没有历史"。
                     */
                    if (
                        diagnostics.none {
                            it is ScheduleRestoreDiagnostic
                                .SnapshotUnreadable &&
                                it.itemId == itemId
                        }
                    ) {
                        diagnostics +=
                            ScheduleRestoreDiagnostic
                                .SnapshotNotFound(
                                    itemId = itemId,
                                )
                    }

                    return@forEach
                }

                snapshots += snapshot

                val traceId =
                    snapshot.traceId

                if (traceId == null) {

                    diagnostics +=
                        ScheduleRestoreDiagnostic
                            .SnapshotWithoutTrace(
                                itemId = itemId,

                                snapshotId = snapshot.id,
                            )

                    return@forEach
                }

                val trace =
                    runCatching {
                        persistence
                            .scheduleTraces
                            .get(traceId)
                    }
                        .getOrElse { failure ->

                            diagnostics +=
                                ScheduleRestoreDiagnostic
                                    .TraceUnreadable(
                                        itemId = itemId,

                                        traceId = traceId,

                                        reason =
                                            failure.describe(),
                                    )

                            null
                        }

                if (trace == null) {

                    if (
                        diagnostics.none {
                            it is ScheduleRestoreDiagnostic
                                .TraceUnreadable &&
                                it.itemId == itemId
                        }
                    ) {
                        diagnostics +=
                            ScheduleRestoreDiagnostic
                                .TraceMissing(
                                    itemId = itemId,

                                    snapshotId = snapshot.id,

                                    traceId = traceId,
                                )
                    }

                    return@forEach
                }

                traces += trace
            }

        return RestoredScheduleState(
            snapshots = snapshots,
            traces = traces,
            diagnostics = diagnostics,
        )
    }

    private fun Throwable.describe(): String =
        message
            ?: this::class.simpleName
            ?: "unknown failure"
}
