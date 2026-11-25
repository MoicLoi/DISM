package me.moiclathy.dism.schedule.migration

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion

/**
 * 13.10.11 / 13.11.5
 *
 * Migration 注册表。
 *
 * 它回答的是"从 A 到 B 有没有现成的迁移路径"，
 * 以及"迁移结果长什么样"。
 *
 * 与 Version Compatibility Registry 的分工：
 *
 * ```text
 * Compatibility Registry → 能不能直接重放
 * Migration Registry     → 不能直接重放时，有没有迁移路径
 * ```
 *
 * 13.11.5：按**来源版本**建索引。
 *
 * 迁移会越来越多（13.1→13.11、13.2→13.11、14.0→14.1 …），
 * 每次都线性扫全部迁移没必要，而且真正需要的是
 * "这个历史版本能迁到当前版本吗"这一个查询形状。
 */
class DynamicScheduleHistoryMigrationRegistry(
    migrations:
        List<DynamicScheduleHistoryMigration> = emptyList(),
) {

    private val migrationsBySource =
        migrations.groupBy {
            it.fromVersion.value
        }

    init {
        require(
            migrations
                .map { it.migrationId }
                .distinct()
                .size == migrations.size,
        ) {
            "migration ids must be unique"
        }
    }

    val size: Int
        get() = migrationsBySource.values.sumOf { it.size }

    fun find(
        fromVersion:
            DynamicScheduleHistoryEngineVersion,

        toVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryMigration? =
        migrationsBySource[
            fromVersion.value
        ]?.firstOrNull { migration ->
            migration.toVersion.value == toVersion.value
        }

    fun require(
        fromVersion:
            DynamicScheduleHistoryEngineVersion,

        toVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryMigration =
        find(
            fromVersion = fromVersion,
            toVersion = toVersion,
        )
            ?: error(
                "No migration registered from " +
                    fromVersion.value +
                    " to " +
                    toVersion.value,
            )

    /**
     * 执行迁移。
     *
     * 返回**新的** ReplayInput 工件；
     * [input] 本身不会被修改（见 [DynamicScheduleHistoryMigration]）。
     */
    fun migrate(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,

        toVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleDecisionReplayInputSnapshot {

        val migration =
            require(
                fromVersion =
                    DynamicScheduleHistoryEngineVersion(
                        input.engineVersion,
                    ),

                toVersion = toVersion,
            )

        return migration.migrate(input)
    }

    val migrationIds: List<String>
        get() =
            migrationsBySource.values
                .flatten()
                .map { it.migrationId }
}
