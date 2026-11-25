package me.moiclathy.dism.schedule.adaptation.audit

import kotlin.time.Instant

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 14.17
 *
 * ```text
 * Snapshot + Trace + Adaptation/Replay/Verification 证据
 *     → Audit
 * ```
 *
 * ### Audit ≠ Event Log（§八）
 *
 * 项目里已经有：
 *
 * ```text
 * TransitionLog      世界状态发生了什么
 * DecisionTrace      一次 Decision 执行了什么
 * ```
 *
 * Audit 是第三件事：
 *
 * ```text
 * 这个 Decision 在它的生命周期里发生了什么
 * ```
 *
 * 所以这里**不复制 Runtime 的每一步**，
 * 只记录"对一个已存在的决策做过什么"。
 */
interface DynamicScheduleDecisionAuditBuilder {

    fun build(
        snapshot:
            DynamicScheduleDecisionSnapshot,

        trace:
            DynamicScheduleDecisionTraceRecord?,

        generatedAt:
            Instant,

        adaptationPersisted:
            Boolean = false,

        replayed:
            Boolean = false,

        verificationStatus:
            String? = null,

        /**
         * 14.17 §九：这个决策是否已经被后续决策取代。
         *
         * 只有调用方真的确认了"存在一个更新的 Snapshot"时才传。
         * Builder 不会自己去猜——猜错就会凭空造出一个
         * 从未发生过的 SUPERSEDED。
         */
        supersededBy:
            DynamicScheduleDecisionSnapshotId? = null,
    ):
        DynamicScheduleDecisionAudit
}
