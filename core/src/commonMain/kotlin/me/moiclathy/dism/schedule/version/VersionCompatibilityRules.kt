package me.moiclathy.dism.schedule.version

/**
 * 13.10.9 / 13.10.10
 *
 * "必须迁移才能 Replay"的版本规则。
 *
 * 与 [ReplayCompatibleVersionRule] 的区别是语义上的：
 *
 * ```text
 * REPLAY_COMPATIBLE → 直接重放，语义没变
 * MIGRATABLE        → 先迁移 ReplayInput，再重放
 * INCOMPATIBLE      → 停止，不存在安全路径
 * ```
 */
class MigratableVersionRule(
    private val historicalVersions: Set<String>,

    private val migrationId: String,

    private val reason: String =
        "The historical engine version requires an explicit replay-input migration.",
) : DynamicScheduleHistoryVersionCompatibilityRule {

    init {
        require(historicalVersions.isNotEmpty()) {
            "MigratableVersionRule needs at least one version"
        }

        require(migrationId.isNotBlank()) {
            "migrationId must not be blank"
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
                    .MIGRATABLE,

            reason = reason,

            migrationId = migrationId,
        )
}

/**
 * 13.10.10
 *
 * 明确不兼容的版本规则。
 *
 * 存在的意义是：把"这个版本之间的决策语义真的变了"
 * 变成一条可审计的显式声明，
 * 而不是让系统在升级后默默重放旧数据。
 */
class IncompatibleVersionRule(
    private val historicalVersions: Set<String>,

    private val reason: String,
) : DynamicScheduleHistoryVersionCompatibilityRule {

    init {
        require(historicalVersions.isNotEmpty()) {
            "IncompatibleVersionRule needs at least one version"
        }

        require(reason.isNotBlank()) {
            "IncompatibleVersionRule needs a reason"
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
                    .INCOMPATIBLE,

            reason = reason,
        )
}
