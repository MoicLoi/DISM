package me.moiclathy.dism.schedule.replay

/**
 * 12.28
 *
 * Replay Verification 发现的具体差异。
 */
data class DynamicScheduleReplayVerificationDifference(
    val factor: String,
    val historicalValue: String,
    val replayedValue: String,
    val description: String,
)
