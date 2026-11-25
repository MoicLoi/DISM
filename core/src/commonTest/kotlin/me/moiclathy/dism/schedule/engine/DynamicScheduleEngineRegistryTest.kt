package me.moiclathy.dism.schedule.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigration
import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigrationResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility
import me.moiclathy.dism.schedule.version.IncompatibleVersionRule
import me.moiclathy.dism.schedule.version.MigratableVersionRule
import me.moiclathy.dism.schedule.version.ReplayCompatibleVersionRule

/**
 * 13.11.16
 *
 * Engine Registry 的行为。
 *
 * 目标是把"当前版本 / 兼容性 / 迁移"三件事收成一个只读入口，
 * 同时**不改变任何既有 API**。
 */
class DynamicScheduleEngineRegistryTest {

    private val current =
        DefaultDynamicScheduleEngineRegistry.ENGINE_VERSION

    // -----------------------------------------------------------------
    // 1. 当前版本
    // -----------------------------------------------------------------

    @Test
    fun currentVersionIsExact() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create()

        assertEquals(current, registry.current.version)

        val compatibility =
            registry.compatibilityFor(current)

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.EXACT,
            compatibility.compatibility,
        )

        assertEquals(
            DynamicScheduleEngineExecutionPlan.DirectReplay,
            registry.executionPlanFor(current),
        )
    }

    @Test
    fun registryExposesExactlyOneCurrentVersion() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create()

        assertEquals(
            registry.current.version,
            registry.currentVersion.value,
        )

        assertTrue(registry.current.displayName.isNotBlank())

        assertTrue(registry.current.description.isNotBlank())
    }

    // -----------------------------------------------------------------
    // 2. 未知版本
    // -----------------------------------------------------------------

    @Test
    fun unknownVersionIsBlocked() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create()

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.UNKNOWN,
            registry.compatibilityFor("13.1").compatibility,
        )

        val plan = registry.executionPlanFor("13.1")

        assertTrue(plan is DynamicScheduleEngineExecutionPlan.Blocked)

        assertFalse(plan.canProceed)
    }

    // -----------------------------------------------------------------
    // 3. 显式兼容
    // -----------------------------------------------------------------

    @Test
    fun registeredCompatibleVersionPlansDirectReplay() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                compatibilityRules =
                    listOf(
                        ReplayCompatibleVersionRule(
                            historicalVersions = setOf("13.1"),
                        ),
                    ),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility
                .REPLAY_COMPATIBLE,
            registry.compatibilityFor("13.1").compatibility,
        )

        assertEquals(
            DynamicScheduleEngineExecutionPlan.DirectReplay,
            registry.executionPlanFor("13.1"),
        )
    }

    // -----------------------------------------------------------------
    // 4 / 5 / 6. 迁移
    // -----------------------------------------------------------------

    @Test
    fun migratableWithoutMigrationIsBlocked() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                compatibilityRules =
                    listOf(
                        MigratableVersionRule(
                            historicalVersions = setOf("13.1"),

                            migrationId = "13.1-to-current",
                        ),
                    ),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.MIGRATABLE,
            registry.compatibilityFor("13.1").compatibility,
        )

        /*
         * 兼容性说"可迁移"，但没有实现——
         * 这比 INCOMPATIBLE 更危险，
         * 因为它会让人以为"只要迁移一下就行"。
         */
        val plan = registry.executionPlanFor("13.1")

        assertTrue(plan is DynamicScheduleEngineExecutionPlan.Blocked)

        assertTrue(
            (plan as DynamicScheduleEngineExecutionPlan.Blocked)
                .reason
                .contains("no migration implementation"),
        )
    }

    @Test
    fun migratableWithMigrationPlansReplayAfterMigration() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                compatibilityRules =
                    listOf(
                        MigratableVersionRule(
                            historicalVersions = setOf("13.1"),

                            migrationId = "13.1-to-current",
                        ),
                    ),

                migrations =
                    listOf(
                        upgradeMigration(
                            from = "13.1",
                            to = current,
                        ),
                    ),
            )

        val plan = registry.executionPlanFor("13.1")

        assertEquals(
            DynamicScheduleEngineExecutionPlan
                .ReplayAfterMigration(
                    migrationId = "13.1-to-$current-v1",
                ),
            plan,
        )
    }

    @Test
    fun migrationProducesNewArtifactAndPreservesOriginal() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                migrations =
                    listOf(
                        upgradeMigration(
                            from = "12.27",
                            to = current,
                        ),
                    ),
            )

        val original =
            DynamicScheduleReplayTestData.create()

        val result = registry.migrate(original)

        assertTrue(
            result is DynamicScheduleHistoryMigrationResult.Migrated,
        )

        val migrated =
            (result as DynamicScheduleHistoryMigrationResult.Migrated)
                .input

        assertEquals(current, migrated.engineVersion)

        /*
         * 原始工件必须一字节未改。
         */
        assertEquals("12.27", original.engineVersion)

        assertTrue(migrated !== original)
    }

    @Test
    fun migrateWithoutMigrationIsAResultNotAnException() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create()

        /*
         * "没有现成迁移"是正常结论，
         * 不应该让整个启动路径炸掉。
         */
        val result =
            registry.migrate(
                DynamicScheduleReplayTestData.create(),
            )

        assertTrue(
            result is DynamicScheduleHistoryMigrationResult
                .NotAvailable,
        )

        assertTrue(
            (result as DynamicScheduleHistoryMigrationResult
                .NotAvailable)
                .reason
                .contains("No migration registered"),
        )
    }

    // -----------------------------------------------------------------
    // 7. 不兼容
    // -----------------------------------------------------------------

    @Test
    fun incompatibleVersionIsBlocked() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                compatibilityRules =
                    listOf(
                        IncompatibleVersionRule(
                            historicalVersions = setOf("12.9"),

                            reason =
                                "Priority formula changed.",
                        ),
                    ),
            )

        assertTrue(
            registry.executionPlanFor("12.9")
                is DynamicScheduleEngineExecutionPlan.Blocked,
        )
    }

    // -----------------------------------------------------------------
    // 8. Registry 不可变
    // -----------------------------------------------------------------

    @Test
    fun registryHasNoMutationApi() {

        /*
         * 13.11.10：Registry 一旦创建就不可变。
         *
         * 这不是"暂时没实现 register()"，
         * 而是 Replay 可复现性的前提：
         *
         * ```text
         * 同一个 Snapshot + 同一个 ReplayInput + 同一个 Registry
         *      = 同一个判定
         * ```
         *
         * 如果运行到一半规则被改写，历史重放就再也无法解释。
         *
         * 类型上没有任何 `register` / `remove` 接口
         * （这一点由编译期保证），
         * 行为上则要求**同一份输入永远得到同一份判定**。
         */
        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                compatibilityRules =
                    listOf(
                        ReplayCompatibleVersionRule(
                            historicalVersions = setOf("13.1"),
                        ),
                    ),
            )

        val first = registry.executionPlanFor("13.1")

        val second = registry.executionPlanFor("13.1")

        assertEquals(first, second)

        assertEquals(
            registry.current,
            registry.current,
        )

        assertTrue(first.canProceed)
    }

    // -----------------------------------------------------------------
    // 9. Registry 只读，且不进入持久化
    // -----------------------------------------------------------------

    @Test
    fun registryDoesNotTouchPersistence() =
        kotlinx.coroutines.test.runTest {

            /*
             * 13.11.12：数据库只保存 engineVersion（以及未来的 migrationId），
             * 不保存整个 Registry。
             *
             * Registry 是"当前 Runtime 的代码配置"，
             * 历史数据库是"过去发生过什么"。
             */
            val persistence = DismPersistence()

            val query =
                me.moiclathy.dism.schedule.snapshot
                    .DynamicScheduleDecisionSnapshotQuery()

            val before =
                persistence.scheduleSnapshots
                    .query(query)
                    .totalMatched

            val registry =
                DefaultDynamicScheduleEngineRegistry.create()

            registry.executionPlanFor("13.1")

            val after =
                persistence.scheduleSnapshots
                    .query(query)
                    .totalMatched

            assertEquals(before, after)
        }

    // -----------------------------------------------------------------
    // 迁移注册表本身
    // -----------------------------------------------------------------

    @Test
    fun migrationRegistryIndexesBySourceVersion() {

        val registry =
            DefaultDynamicScheduleEngineRegistry.create(
                migrations =
                    listOf(
                        /* 通往当前版本：命中。 */
                        upgradeMigration(
                            from = "12.27",
                            to = current,
                        ),

                        /* 目标是 14.0：在 current = 13.11 下不该命中。 */
                        upgradeMigration(from = "13.1", to = "14.0"),

                        upgradeMigration(from = "13.2", to = "14.0"),
                    ),
            )

        /*
         * 多个来源版本可以共存，
         * 但只有"通往当前版本"的那条会被找到。
         */
        assertEquals(3, registry.migrationIds.size)

        assertNotNull(
            registry.migrationFor("12.27"),
        )

        assertNull(
            registry.migrationFor("13.1"),
            "13.1 → 14.0 不是通往当前版本的迁移，不应命中",
        )

        assertNull(registry.migrationFor("13.2"))

        assertNull(registry.migrationFor("9.9"))
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private fun upgradeMigration(
        from: String,
        to: String,
    ): DynamicScheduleHistoryMigration =
        object : DynamicScheduleHistoryMigration {

            override val migrationId: String =
                "$from-to-$to-v1"

            override val fromVersion =
                DynamicScheduleHistoryEngineVersion(from)

            override val toVersion =
                DynamicScheduleHistoryEngineVersion(to)

            override fun migrate(
                input:
                    DynamicScheduleDecisionReplayInputSnapshot,
            ): DynamicScheduleDecisionReplayInputSnapshot =
                input.copy(engineVersion = to)
        }
}
