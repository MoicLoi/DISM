package me.moiclathy.dism.schedule.engine

import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigration
import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigrationRegistry
import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigrationResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityRegistry
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityResult

/**
 * 13.11.3 / 13.11.6
 *
 * Dynamic Schedule Engine Registry。
 *
 * 它把三件本来会散落各处的东西收在一起：
 *
 * ```text
 *                  DynamicScheduleEngineRegistry
 *                             │
 *              ┌──────────────┼──────────────┐
 *              ▼              ▼              ▼
 *         Engine Version   Compatibility   Migration
 *              │              │              │
 *              └──────────────┼──────────────┘
 *                             ▼
 *                     Replay / Verify
 * ```
 *
 * ### 为什么是只读的（13.11.10）
 *
 * 一旦创建，`current version` / 兼容规则 / 迁移规则就**不再变化**：
 *
 * ```text
 * 同一个历史 Snapshot
 *   + 同一个 ReplayInput
 *   + 同一个 Engine Registry
 *   = 同一个判定结果
 * ```
 *
 * 如果运行到一半规则被动态改写，就会出现
 *
 * ```text
 * 第一次 Replay：13.1 → compatible
 * 第二次 Replay：13.1 → incompatible
 * ```
 *
 * 这种无法解释、也无法审计的行为。
 * 12.29 已经建立了 Replay Determinism，
 * 13.11 只是把同一条原则延伸到版本层：
 *
 * ```text
 * Replay Determinism + Registry Immutability
 *        ↓
 * Historical Replay Reproducibility
 * ```
 *
 * 因此这里**没有** `register(...)` / `remove(...)`。
 *
 * ### 数据库里不保存 Registry（13.11.12）
 *
 * 历史表只保存 `engineVersion`（以及未来的 `migrationId`）。
 * Registry 是"当前 Runtime 的代码配置"，
 * 历史表是"过去发生过什么"，两者职责不能混。
 */
class DynamicScheduleEngineRegistry(
    val current:
        DynamicScheduleEngineDescriptor,

    /**
     * 兼容性注册表本身是只读接口（只有 `resolve`），
     * 暴露出来是为了让 Replay 这一类**底层服务**能直接做判断，
     * 而不必反向依赖 Engine Registry（那会形成包循环）。
     */
    val compatibilityRegistry:
        DynamicScheduleHistoryVersionCompatibilityRegistry,

    private val migrationRegistry:
        DynamicScheduleHistoryMigrationRegistry,
) {

    val currentVersion:
        DynamicScheduleHistoryEngineVersion
        get() =
            DynamicScheduleHistoryEngineVersion(
                current.version,
            )

    /**
     * 历史版本与当前版本的兼容性。
     */
    fun compatibilityFor(
        historicalVersion: String,
    ): DynamicScheduleHistoryVersionCompatibilityResult =
        compatibilityRegistry.resolve(
            historicalVersion =
                DynamicScheduleHistoryEngineVersion(
                    historicalVersion,
                ),

            currentVersion = currentVersion,
        )

    /**
     * 从历史版本到当前版本的迁移实现。
     */
    fun migrationFor(
        historicalVersion: String,
    ): DynamicScheduleHistoryMigration? =
        migrationRegistry.find(
            fromVersion =
                DynamicScheduleHistoryEngineVersion(
                    historicalVersion,
                ),

            toVersion = currentVersion,
        )

    /**
     * 13.11.8：把版本策略细节收成一个执行计划。
     *
     * 上层不需要知道 EXACT / REPLAY_COMPATIBLE / MIGRATABLE /
     * INCOMPATIBLE / UNKNOWN 这五个枚举怎么分支。
     */
    fun executionPlanFor(
        historicalVersion: String,
    ): DynamicScheduleEngineExecutionPlan {

        val compatibility =
            compatibilityFor(historicalVersion)

        return when (compatibility.compatibility) {

            DynamicScheduleHistoryVersionCompatibility.EXACT,
            DynamicScheduleHistoryVersionCompatibility
                .REPLAY_COMPATIBLE ->
                DynamicScheduleEngineExecutionPlan.DirectReplay

            DynamicScheduleHistoryVersionCompatibility.MIGRATABLE -> {

                val migration =
                    migrationFor(historicalVersion)

                if (migration == null) {

                    /*
                     * 兼容性说是"可迁移"，但没有实现：
                     * 这比 INCOMPATIBLE 更危险，
                     * 因为它会让人以为"只需要迁移一下"。
                     */
                    DynamicScheduleEngineExecutionPlan.Blocked(
                        reason =
                            "Version is migratable, but no migration implementation is registered.",
                    )
                } else {

                    DynamicScheduleEngineExecutionPlan
                        .ReplayAfterMigration(
                            migrationId =
                                migration.migrationId,
                        )
                }
            }

            DynamicScheduleHistoryVersionCompatibility
                .INCOMPATIBLE ->
                DynamicScheduleEngineExecutionPlan.Blocked(
                    reason =
                        "Historical engine version is incompatible with the current engine.",
                )

            DynamicScheduleHistoryVersionCompatibility.UNKNOWN ->
                DynamicScheduleEngineExecutionPlan.Blocked(
                    reason =
                        "Historical engine version is unknown.",
                )
        }
    }

    /**
     * 13.11.9：迁移一个历史 ReplayInput。
     *
     * 返回结果而不是抛异常：
     * "没有现成迁移"是一个**正常结论**，
     * 不应该让整个启动路径失败。
     *
     * 迁移**不会**修改 [input]，
     * 返回的是新工件（Original + Migrated Artifact 并存）。
     */
    fun migrate(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleHistoryMigrationResult {

        val migration =
            migrationFor(input.engineVersion)

        if (migration == null) {

            return DynamicScheduleHistoryMigrationResult
                .NotAvailable(
                    reason =
                        "No migration registered from " +
                            input.engineVersion +
                            " to " +
                            current.version +
                            ".",
                )
        }

        return DynamicScheduleHistoryMigrationResult
            .Migrated(
                input = migration.migrate(input),
                migrationId = migration.migrationId,
            )
    }

    val migrationIds: List<String>
        get() = migrationRegistry.migrationIds
}
