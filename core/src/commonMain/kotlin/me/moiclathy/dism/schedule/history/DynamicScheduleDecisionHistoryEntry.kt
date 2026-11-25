package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenance
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationReport
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 12.x / 13.13
 *
 * 某一次历史 Decision 的统一视图。
 *
 * ```text
 * Snapshot      历史事实（当时发生过什么）
 * Trace         历史决策链（当时为什么这样决定）
 * Provenance    后来重新运行过这件事的记录（持久化）
 * Replay        本次查询真正执行的重放结果（可选）
 * Verification  本次查询真正执行的验证结果（可选）
 * ```
 *
 * 它们**不会互相覆盖**：
 *
 * - [snapshot] / [trace] 是不可变的历史证据；
 * - [provenance] 是过去某次重放留下的记录；
 * - [replay] / [verification] 只在查询显式要求时才有值。
 *
 * ### 三个不同的时间（13.13.11）
 *
 * ```text
 * capturedAt     = 当时系统做这个 Decision 的时间
 * lastReplayedAt = 后来重新运行它的时间
 * ```
 *
 * 本类型刻意**不**把它们合并成一个 `time`：
 * 把"历史发生过什么"和"现在重新算出什么"混为一谈，
 * 正是 DISM 一路在避免的事。
 */
data class DynamicScheduleDecisionHistoryEntry(
    val snapshot: DynamicScheduleDecisionSnapshot,

    val trace: DynamicScheduleDecisionTraceRecord?,

    /**
     * 持久化的来源链记录（如果这条历史被重放过）。
     */
    val provenance: DynamicScheduleDecisionProvenance? = null,

    /**
     * 本次查询真正执行的重放结果。
     *
     * 默认查询里为 null——查询历史不等于重新执行历史。
     */
    val replay: DynamicScheduleHistoricalRuntimeReplayResult? = null,

    val verification: DynamicScheduleReplayVerificationReport? = null,
) {

    val itemId
        get() = snapshot.itemId

    val traceId
        get() = snapshot.traceId

    val snapshotId
        get() = snapshot.id

    /**
     * 原始决策时间。**不是**重放时间。
     */
    val capturedAt
        get() = snapshot.capturedAt

    val scheduleRevision
        get() = snapshot.scheduleRevision

    val engineVersion
        get() = snapshot.engineVersion

    val hasTrace: Boolean
        get() = trace != null

    /**
     * 这条历史**曾经**被重放过（有持久化的来源链）。
     */
    val wasReplayed: Boolean
        get() = provenance != null

    val hasReplay: Boolean
        get() = replay != null

    val hasVerification: Boolean
        get() = verification != null

    /**
     * 已知的验证状态。
     *
     * 优先用本次查询真正跑出来的结果；
     * 没有的话退回来源链里记录的上一次验证状态。
     *
     * 这样默认查询（不重放）也能回答
     * "这条历史上次验证是什么结论"，而不必再跑一遍 Runtime。
     */
    val knownVerificationStatus: String?
        get() =
            verification?.status?.name
                ?: provenance?.verificationStatus

    /**
     * 最近一次重放发生的时间。
     *
     * 与 [capturedAt] 是两件事，不要混用。
     */
    val lastReplayedAt: kotlin.time.Instant?
        get() =
            replay?.execution?.replayedAt
                ?: provenance?.replayedAt
}
