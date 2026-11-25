package me.moiclathy.dism.schedule.adaptation.verification

/**
 * 14.13
 *
 * 一次 Adaptation Verification 的结论。
 *
 * `verified` 只在 `EXACT_MATCH` 时为 true——
 * 这是一个刻意的严格定义：
 *
 * ```text
 * INPUT_CHANGED / POLICY_CHANGED / ENGINE_CHANGED
 * ```
 *
 * 都**不是**"验证通过"，而是"这次验证无法证明行为没变"。
 */
data class DynamicScheduleAdaptationVerificationReport(
    val status:
        DynamicScheduleAdaptationVerificationStatus,

    val historicalEngineVersion: String,

    val replayEngineVersion: String,

    val historicalPolicyFingerprint: String,

    val replayPolicyFingerprint: String,

    val differences:
        List<DynamicScheduleAdaptationVerificationDifference> =
        emptyList(),

    val diagnostics:
        List<String> =
        emptyList(),
) {

    val verified: Boolean
        get() =
            status ==
                DynamicScheduleAdaptationVerificationStatus
                    .EXACT_MATCH

    val changed: Boolean
        get() =
            status !=
                DynamicScheduleAdaptationVerificationStatus
                    .EXACT_MATCH

    val differenceCount: Int
        get() = differences.size
}
