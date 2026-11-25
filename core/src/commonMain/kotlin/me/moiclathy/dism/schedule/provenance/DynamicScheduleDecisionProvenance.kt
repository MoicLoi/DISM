package me.moiclathy.dism.schedule.provenance

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 13.12.4
 *
 * Decision Provenance —— 一次历史决策**被重新运行过**这件事的记录。
 *
 * 它与历史证据严格分开：
 *
 * ```text
 * Snapshot = 当时发生过什么      （不可变）
 * Trace    = 当时为什么这样决定   （不可变）
 * Provenance = 后来重新运行时发生了什么
 * ```
 *
 * ### 与 ReplayReport 的区别（13.12.5）
 *
 * ```text
 * ReplayReport 回答："Replay 的结果怎么样？"
 * Provenance   回答："这次 Replay 从哪里来、经过什么版本路径？"
 * ```
 *
 * ### 字段说明
 *
 * - [replayInputSnapshotId] 复用
 *   [DynamicScheduleDecisionSnapshotId]：
 *   当前 ReplayInput Store 的主键就是 `decisionSnapshot.id`，
 *   没必要为 Provenance 再造一套 ID。
 * - [verificationStatus] 可以为 null：
 *   Replay 先发生、Verification 后发生（13.12.14）。
 */
data class DynamicScheduleDecisionProvenance(
    val id:
        DynamicScheduleDecisionProvenanceId =
        DynamicScheduleDecisionProvenanceId(),

    val itemId:
        DynamicScheduleItemId,

    val snapshotId:
        DynamicScheduleDecisionSnapshotId,

    val traceId:
        DynamicScheduleDecisionTraceId?,

    val replayInputSnapshotId:
        DynamicScheduleDecisionSnapshotId?,

    val historicalEngineVersion:
        String,

    val replayEngineVersion:
        String?,

    val compatibility:
        DynamicScheduleHistoryVersionCompatibility,

    val migrationId:
        String?,

    val createdAt:
        Instant,

    val replayedAt:
        Instant?,

    /**
     * 13.16：本次重放结果的行为指纹。
     *
     * 由 [me.moiclathy.dism.schedule.replay
     * .DynamicScheduleReplayDeterminismFingerprint] 生成，
     * **不包含** [replayedAt]——
     * 否则"同一历史重放两次"会天然得到两个不同指纹，
     * 让"行为有没有变"永远回答"变了"。
     */
    val behaviorFingerprint: String? = null,

    /**
     * 13.16：这一次校验的事实。
     *
     * Replay 先发生、Verification 后发生（13.12.14），
     * 所以这里可以是 null；校验完成后再回填。
     *
     * 13.12 原本只有一个 `verificationStatus: String`，
     * 13.16 升级成结构化记录，以便同时保存
     * `verifiedAt` / `differenceCount` / `diagnostics`。
     * 旧的 [verificationStatus] 访问器仍然可用。
     */
    val verification:
        DynamicScheduleReplayVerificationProvenance? = null,
) {

    init {
        require(historicalEngineVersion.isNotBlank()) {
            "Historical engine version must not be blank"
        }

        require(
            replayEngineVersion == null ||
                replayEngineVersion.isNotBlank(),
        ) {
            "Replay engine version must not be blank when present"
        }

        require(
            behaviorFingerprint == null ||
                behaviorFingerprint.isNotBlank(),
        ) {
            "behaviorFingerprint must not be blank when present"
        }

        require(migrationId == null || migrationId.isNotBlank()) {
            "migrationId must not be blank when present"
        }
    }

    /**
     * 是否已经做过 Verification。
     */
    val verified: Boolean
        get() = verification != null

    /**
     * 校验状态（字符串形式）。
     *
     * 保留 13.12 的访问器形状，但唯一来源是 [verification]。
     */
    val verificationStatus: String?
        get() = verification?.status?.name

    /**
     * 历史版本与重放版本是否相同。
     *
     * 注意：不同不代表有问题——可能只是显式声明了
     * [DynamicScheduleHistoryVersionCompatibility.REPLAY_COMPATIBLE]。
     */
    val replayedWithDifferentEngine: Boolean
        get() =
            replayEngineVersion != null &&
                replayEngineVersion != historicalEngineVersion
}
