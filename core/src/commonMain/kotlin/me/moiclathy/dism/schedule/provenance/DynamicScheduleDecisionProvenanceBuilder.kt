package me.moiclathy.dism.schedule.provenance

import kotlin.time.Instant

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 13.12.13
 *
 * Provenance 组装器。
 *
 * 单独抽出来的目的：不让 Replay Service 越变越肥。
 *
 * ### 一处对设计稿的调整
 *
 * 设计稿的 `build(...)` 直接接收
 * `DynamicScheduleDecisionReplayInputSnapshot`。
 *
 * 那会让 `provenance` 包依赖 `replay` 包，而 13.12.15 又要求
 * Replay 结果里带上 `provenanceId` —— 于是 `provenance ↔ replay` 成环，
 * 把 13.2 / 13.10 / 13.11 一路守下来的依赖方向破掉。
 *
 * 所以这里只接收**它真正需要的东西**：
 * 一条 Snapshot（历史证据本身）+ 几个原始值。
 *
 * 调用方（Replay Service）本来就有 `replayInput.decisionSnapshot`
 * 和 `replayInput.engineVersion`，多传两个参数没有任何损失。
 *
 * 这样分层是：
 *
 * ```text
 * provenance → { version, model, snapshot, trace }
 * replay     → { version, provenance, snapshot, trace, runtime }
 * ```
 *
 * 无环。
 *
 * ### itemId 的来源
 *
 * 直接取 `snapshot.itemId`，而**不是**从
 * `replayInput.input.model.items` 里猜
 * （见 13.12.12：model 是完整 Schedule Model，
 * 里面可能有多个 item，猜错就是错误的溯源）。
 */
class DynamicScheduleDecisionProvenanceBuilder {

    fun build(
        snapshot:
            DynamicScheduleDecisionSnapshot,

        historicalEngineVersion: String,

        replayEngineVersion: String?,

        compatibility:
            DynamicScheduleHistoryVersionCompatibility,

        migrationId: String?,

        evaluatedAt: Instant,

        replayedAt: Instant?,

        /**
         * 13.16：重放结果的行为指纹。
         */
        behaviorFingerprint: String? = null,

        verification:
            DynamicScheduleReplayVerificationProvenance? = null,
    ): DynamicScheduleDecisionProvenance =
        DynamicScheduleDecisionProvenance(
            itemId = snapshot.itemId,

            snapshotId = snapshot.id,

            traceId = snapshot.traceId,

            replayInputSnapshotId = snapshot.id,

            historicalEngineVersion =
                historicalEngineVersion,

            replayEngineVersion =
                replayEngineVersion,

            compatibility = compatibility,

            migrationId = migrationId,

            /*
             * createdAt 是这条 provenance **记录**被创建的时间。
             *
             * 13.15 之后它由调用方传入的真实重放时间决定，
             * 而不是历史求值时间——记录是"后来"才产生的。
             */
            createdAt = evaluatedAt,

            replayedAt = replayedAt,

            behaviorFingerprint = behaviorFingerprint,

            verification = verification,
        )
}
