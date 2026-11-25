package me.moiclathy.dism.schedule.version

/**
 * 13.10.16
 *
 * 版本兼容性诊断。
 *
 * 存在的唯一理由是：**UI 不应该被迫理解 Enum**。
 *
 * App 拿到这个以后可以直接渲染：
 *
 * ```text
 * 历史决策版本：13.1
 * 当前引擎版本：13.10
 *
 * 状态：未知兼容性
 * Replay：禁止
 *
 * 原因：当前没有注册 13.1 → 13.10 的兼容性规则。
 * ```
 */
data class DynamicScheduleHistoryVersionDiagnostic(
    val compatibility:
        DynamicScheduleHistoryVersionCompatibilityResult,

    val replayAllowed: Boolean,

    val migrationRequired: Boolean,

    val message: String,
) {

    init {
        require(message.isNotBlank()) {
            "Version diagnostic message must not be blank"
        }

        /*
         * 迁移与直接重放互斥：
         * MIGRATABLE 的状态下不允许直接放行 Replay。
         */
        require(!(replayAllowed && migrationRequired)) {
            "Replay cannot be both allowed and pending migration"
        }
    }

    companion object {

        fun from(
            compatibility:
                DynamicScheduleHistoryVersionCompatibilityResult,
        ): DynamicScheduleHistoryVersionDiagnostic {

            val replayAllowed =
                compatibility.canReplayDirectly

            val migrationRequired =
                compatibility.requiresMigration

            return DynamicScheduleHistoryVersionDiagnostic(
                compatibility = compatibility,

                replayAllowed = replayAllowed,

                migrationRequired = migrationRequired,

                message =
                    buildString {
                        append("Historical engine version ")
                        append(
                            compatibility
                                .historicalVersion
                                .value,
                        )
                        append(", current engine version ")
                        append(
                            compatibility
                                .currentVersion
                                .value,
                        )
                        append(". Compatibility: ")
                        append(compatibility.compatibility)
                        append(". Replay: ")

                        append(
                            when {
                                replayAllowed -> "allowed"

                                migrationRequired ->
                                    "blocked until migration"

                                else -> "blocked"
                            },
                        )

                        append(". Reason: ")
                        append(compatibility.reason)
                    },
            )
        }
    }
}
