package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

class DeterministicDynamicScheduleDecisionReplayReportBuilder :
    DynamicScheduleDecisionReplayReportBuilder {

    override fun build(
        replay:
            DynamicScheduleSnapshotReplayResult,

        originalTrace:
            DynamicScheduleDecisionTraceRecord?,
    ): DynamicScheduleDecisionReplayReport {

        val changedFactors =
            replay.differences.map {
                toChangedFactor(it)
            }

        val severity =
            determineSeverity(replay)

        val summary =
            buildSummary(
                replay = replay,
                severity = severity,
            )

        return DynamicScheduleDecisionReplayReport(
            snapshot =
                replay.snapshot,

            originalTrace =
                originalTrace,

            originalPriority =
                replay.originalPriority,

            currentPriority =
                replay.currentPriority,

            originalAttentionRequired =
                replay.originalAttentionRequired,

            currentAttentionRequired =
                replay.currentAttentionRequired,

            severity =
                severity,

            changedFactors =
                changedFactors,

            differences =
                replay.differences,

            summary =
                summary,
        )
    }

    private fun toChangedFactor(
        difference:
            DynamicScheduleSnapshotReplayDifference,
    ): DynamicScheduleDecisionReplayChangedFactor {

        val impact =
            when (difference.type) {

                DynamicScheduleSnapshotReplayDifferenceType
                    .PRIORITY_CHANGED ->
                    "Attention priority changed"

                DynamicScheduleSnapshotReplayDifferenceType
                    .ATTENTION_DECISION_CHANGED ->
                    "Whether attention is required changed"

                DynamicScheduleSnapshotReplayDifferenceType
                    .CONFLICT_CHANGED ->
                    "Decision constraint state changed"
            }

        return DynamicScheduleDecisionReplayChangedFactor(
            name =
                difference.type.name,

            originalValue =
                difference.originalValue,

            currentValue =
                difference.currentValue,

            impact =
                impact,
        )
    }

    private fun determineSeverity(
        replay:
            DynamicScheduleSnapshotReplayResult,
    ): DynamicScheduleDecisionReplaySeverity {

        if (replay.equivalent) {
            return DynamicScheduleDecisionReplaySeverity.NONE
        }

        if (
            replay.attentionDecisionChanged &&
            replay.conflictChanged
        ) {
            return DynamicScheduleDecisionReplaySeverity
                .CRITICAL
        }

        if (
            replay.attentionDecisionChanged
        ) {
            return DynamicScheduleDecisionReplaySeverity
                .SIGNIFICANT
        }

        if (
            replay.priorityChanged ||
            replay.conflictChanged
        ) {
            return DynamicScheduleDecisionReplaySeverity
                .BEHAVIOR_CHANGED
        }

        return DynamicScheduleDecisionReplaySeverity
            .INFORMATIONAL
    }

    private fun buildSummary(
        replay:
            DynamicScheduleSnapshotReplayResult,

        severity:
            DynamicScheduleDecisionReplaySeverity,
    ): String {

        if (replay.equivalent) {
            return "Historical decision remains equivalent under replay."
        }

        if (
            replay.attentionDecisionChanged
        ) {
            return if (
                replay.currentAttentionRequired
            ) {
                "Replay now requires attention where the historical decision did not."
            } else {
                "Replay no longer requires attention where the historical decision did."
            }
        }

        if (
            replay.priorityChanged
        ) {
            return "Decision priority changed during replay."
        }

        return when (severity) {

            DynamicScheduleDecisionReplaySeverity
                .INFORMATIONAL ->
                "Historical decision has informational differences."

            DynamicScheduleDecisionReplaySeverity
                .BEHAVIOR_CHANGED ->
                "Decision behavior changed during replay."

            DynamicScheduleDecisionReplaySeverity
                .SIGNIFICANT ->
                "Decision changed significantly during replay."

            DynamicScheduleDecisionReplaySeverity
                .CRITICAL ->
                "Decision and constraint behavior changed during replay."

            DynamicScheduleDecisionReplaySeverity
                .NONE ->
                "Historical decision remains equivalent under replay."
        }
    }
}
