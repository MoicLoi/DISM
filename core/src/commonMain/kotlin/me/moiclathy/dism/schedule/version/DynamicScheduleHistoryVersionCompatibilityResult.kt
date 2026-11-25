package me.moiclathy.dism.schedule.version

/**
 * 13.10.4
 *
 * 一次版本兼容性判断的结果。
 */
data class DynamicScheduleHistoryVersionCompatibilityResult(
    val historicalVersion:
        DynamicScheduleHistoryEngineVersion,

    val currentVersion:
        DynamicScheduleHistoryEngineVersion,

    val compatibility:
        DynamicScheduleHistoryVersionCompatibility,

    val reason: String,

    /**
     * 当 [compatibility] 是 [DynamicScheduleHistoryVersionCompatibility.MIGRATABLE] 时，
     * 指向可以完成迁移的 Migration id。
     */
    val migrationId: String? = null,
) {

    init {
        require(reason.isNotBlank()) {
            "Compatibility reason must not be blank"
        }

        require(
            compatibility ==
                DynamicScheduleHistoryVersionCompatibility.MIGRATABLE ||
                migrationId == null,
        ) {
            "migrationId is only meaningful for MIGRATABLE compatibility"
        }

        require(
            compatibility !=
                DynamicScheduleHistoryVersionCompatibility.MIGRATABLE ||
                migrationId != null,
        ) {
            "MIGRATABLE compatibility must name a migration"
        }
    }

    val canReplayDirectly: Boolean
        get() =
            compatibility ==
                DynamicScheduleHistoryVersionCompatibility.EXACT ||
                compatibility ==
                DynamicScheduleHistoryVersionCompatibility
                    .REPLAY_COMPATIBLE

    val requiresMigration: Boolean
        get() =
            compatibility ==
                DynamicScheduleHistoryVersionCompatibility.MIGRATABLE

    val isBlocked: Boolean
        get() = !canReplayDirectly

    companion object {

        /**
         * 便捷构造：两个版本完全相同。
         */
        fun exact(
            version: DynamicScheduleHistoryEngineVersion,
        ): DynamicScheduleHistoryVersionCompatibilityResult =
            DynamicScheduleHistoryVersionCompatibilityResult(
                historicalVersion = version,
                currentVersion = version,
                compatibility =
                    DynamicScheduleHistoryVersionCompatibility.EXACT,
                reason =
                    "Historical and current engine versions are identical.",
            )
    }
}
