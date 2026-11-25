package me.moiclathy.dism.schedule.engine

import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigration
import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigrationRegistry
import me.moiclathy.dism.schedule.version.DefaultDynamicScheduleHistoryVersionCompatibilityRegistry
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityRule

/**
 * 13.11.4
 *
 * 默认 Engine Registry 工厂。
 *
 * **这是全项目唯一一处写死当前引擎版本的地方。**
 *
 * 以后升级到 13.12 只需要改 [ENGINE_VERSION]，
 * 而不是在 Assembly / Runtime / Replay / Verification 里到处改字符串。
 *
 * 默认兼容规则为**空**：
 *
 * ```text
 * 13.11 → 13.11  EXACT
 * 其它版本        UNKNOWN（不能直接重放）
 * ```
 *
 * 也就是说 13.11 默认只认识自己。
 * 以后真的确认某两个版本之间"只改了内部实现、
 * 没有改变 Replay 所依赖的语义"，再显式注册一条规则。
 *
 * 这里刻意**不做**自动推断：
 *
 * ```text
 * 13.1 → 13.11  = 兼容？   ← 不能靠版本号相近来回答
 * ```
 */
object DefaultDynamicScheduleEngineRegistry {

    /**
     * 当前引擎版本。
     *
     * 13.11：它同时是 Decision Snapshot / Trace / ReplayInput
     * 写入时记录的 `engineVersion`，
     * 也是 Replay 与 Verification 使用的引擎版本。
     */
    const val ENGINE_VERSION: String = "13.11"

    fun create(
        migrations:
            List<DynamicScheduleHistoryMigration> =
            emptyList(),

        compatibilityRules:
            List<DynamicScheduleHistoryVersionCompatibilityRule> =
            emptyList(),
    ): DynamicScheduleEngineRegistry {

        val engine =
            DynamicScheduleEngineDescriptor(
                version = ENGINE_VERSION,

                displayName =
                    "DISM Dynamic Schedule Engine",

                description =
                    "Deterministic dynamic schedule decision engine.",
            )

        return DynamicScheduleEngineRegistry(
            current = engine,

            compatibilityRegistry =
                DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                    currentVersion =
                        DynamicScheduleHistoryEngineVersion(
                            engine.version,
                        ),

                    rules = compatibilityRules,
                ),

            migrationRegistry =
                DynamicScheduleHistoryMigrationRegistry(
                    migrations = migrations,
                ),
        )
    }
}
