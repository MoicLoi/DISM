package me.moiclathy.dism.schedule.version

/**
 * 13.10.9
 *
 * "已确认对 Replay 兼容"的版本规则。
 *
 * 用法（**以后**真的确认之后再注册）：
 *
 * ```kotlin
 * ReplayCompatibleVersionRule(
 *     historicalVersions = setOf("13.1", "13.2", "13.3"),
 * )
 * ```
 *
 * 注意：现在**不能**凭空把 13.1–13.10 全标成兼容。
 * 只有确认"这些版本之间只改了内部 API、
 * 没有改变 Replay 所依赖的 Decision semantics"之后才注册。
 */
class ReplayCompatibleVersionRule(
    private val historicalVersions: Set<String>,

    private val reason: String =
        "The historical engine version is explicitly registered as replay-compatible.",
) : DynamicScheduleHistoryVersionCompatibilityRule {

    init {
        require(historicalVersions.isNotEmpty()) {
            "ReplayCompatibleVersionRule needs at least one version"
        }

        require(
            historicalVersions.none { it.isBlank() },
        ) {
            "Registered versions must not be blank"
        }
    }

    override fun matches(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,

        currentVersion:
            DynamicScheduleHistoryEngineVersion,
    ): Boolean =
        historicalVersion.value in historicalVersions

    override fun resolve(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,

        currentVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryVersionCompatibilityResult =
        DynamicScheduleHistoryVersionCompatibilityResult(
            historicalVersion = historicalVersion,

            currentVersion = currentVersion,

            compatibility =
                DynamicScheduleHistoryVersionCompatibility
                    .REPLAY_COMPATIBLE,

            reason = reason,
        )
}
