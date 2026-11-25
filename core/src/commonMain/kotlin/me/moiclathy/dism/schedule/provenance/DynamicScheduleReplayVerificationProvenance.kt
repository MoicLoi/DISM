package me.moiclathy.dism.schedule.provenance

import kotlin.time.Instant

import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationReport
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus

/**
 * 13.16.3
 *
 * 一次校验留下的**事实**。
 *
 * 刻意不把整个
 * [DynamicScheduleReplayVerificationReport] 原封不动塞进数据库：
 *
 * - `differences` 的完整差异列表属于**报告**，不是 provenance；
 * - provenance 只需要"有没有差异、有几条"以及诊断文本。
 *
 * 这样数据库不会变成第二套 Runtime State。
 */
data class DynamicScheduleReplayVerificationProvenance(
    val verifiedAt:
        Instant,

    val status:
        DynamicScheduleReplayVerificationStatus,

    val historicalEngineVersion:
        String,

    val replayEngineVersion:
        String,

    val differenceCount:
        Int,

    val diagnostics:
        List<String> = emptyList(),
) {

    init {
        require(differenceCount >= 0) {
            "differenceCount must be >= 0"
        }

        require(historicalEngineVersion.isNotBlank()) {
            "historicalEngineVersion must not be blank"
        }

        require(replayEngineVersion.isNotBlank()) {
            "replayEngineVersion must not be blank"
        }
    }

    companion object {

        fun from(
            report: DynamicScheduleReplayVerificationReport,
        ): DynamicScheduleReplayVerificationProvenance =
            DynamicScheduleReplayVerificationProvenance(
                verifiedAt = report.verifiedAt,

                status = report.status,

                historicalEngineVersion =
                    report.historicalEngineVersion,

                replayEngineVersion =
                    report.replayEngineVersion,

                differenceCount = report.differenceCount,

                diagnostics = report.diagnostics,
            )
    }
}
