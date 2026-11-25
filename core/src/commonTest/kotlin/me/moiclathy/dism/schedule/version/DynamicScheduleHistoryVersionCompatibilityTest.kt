package me.moiclathy.dism.schedule.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigration
import me.moiclathy.dism.schedule.migration.DynamicScheduleHistoryMigrationRegistry
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData

/**
 * 13.10.17
 *
 * 版本兼容性。
 *
 * 这一组测试守的是一条原则：
 *
 * > **不知道就不能直接重放。**
 *
 * 以及一条更重要的边界：
 *
 * > **迁移不修改原始历史工件。**
 */
class DynamicScheduleHistoryVersionCompatibilityTest {

    private val current =
        DynamicScheduleHistoryEngineVersion("13.10")

    // -----------------------------------------------------------------
    // 1. Same version
    // -----------------------------------------------------------------

    @Test
    fun identicalVersionsAreExact() {

        val result =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,
            ).resolve(
                DynamicScheduleHistoryEngineVersion("13.10"),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.EXACT,
            result.compatibility,
        )

        assertTrue(result.canReplayDirectly)

        assertFalse(result.requiresMigration)

        assertFalse(result.isBlocked)
    }

    // -----------------------------------------------------------------
    // 2. Unknown version
    // -----------------------------------------------------------------

    @Test
    fun unregisteredVersionsAreUnknownAndBlocked() {

        val result =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,
            ).resolve(
                DynamicScheduleHistoryEngineVersion("13.1"),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.UNKNOWN,
            result.compatibility,
        )

        assertFalse(
            result.canReplayDirectly,
            "没有注册规则的版本默认不能直接重放",
        )

        assertTrue(result.isBlocked)
    }

    @Test
    fun similarVersionNumbersAreNotAssumedCompatible() {

        /*
         * "13.1 和 13.10 看起来很近" 是字符串直觉。
         *
         * 这条测试把这个直觉钉死：
         * 不做前缀比较、不做数值比较。
         */
        val result =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,
            ).resolve(
                DynamicScheduleHistoryEngineVersion("13.2"),
            )

        assertFalse(result.canReplayDirectly)
    }

    // -----------------------------------------------------------------
    // 3. Explicit replay compatible
    // -----------------------------------------------------------------

    @Test
    fun registeredVersionsBecomeReplayCompatible() {

        val registry =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,

                rules =
                    listOf(
                        ReplayCompatibleVersionRule(
                            historicalVersions =
                                setOf("13.1", "13.2", "13.3"),
                        ),
                    ),
            )

        val result =
            registry.resolve(
                DynamicScheduleHistoryEngineVersion("13.1"),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility
                .REPLAY_COMPATIBLE,
            result.compatibility,
        )

        assertTrue(result.canReplayDirectly)

        /* 未注册的版本仍然保守。 */
        assertTrue(
            registry
                .resolve(
                    DynamicScheduleHistoryEngineVersion("12.9"),
                )
                .isBlocked,
        )
    }

    @Test
    fun exactMatchWinsOverRegisteredRules() {

        /*
         * 同名版本之间不存在"兼容性"问题，
         * 即使它同时命中了一条 REPLAY_COMPATIBLE 规则。
         */
        val registry =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,

                rules =
                    listOf(
                        ReplayCompatibleVersionRule(
                            historicalVersions =
                                setOf("13.10"),
                        ),
                    ),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.EXACT,
            registry
                .resolve(
                    DynamicScheduleHistoryEngineVersion("13.10"),
                )
                .compatibility,
        )
    }

    // -----------------------------------------------------------------
    // 4. Explicit migratable
    // -----------------------------------------------------------------

    @Test
    fun registeredMigrationBecomesMigratable() {

        val registry =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion =
                    DynamicScheduleHistoryEngineVersion("14.0"),

                rules =
                    listOf(
                        MigratableVersionRule(
                            historicalVersions = setOf("13.10"),

                            migrationId =
                                "13.10-to-14.0-replay-input-v1",
                        ),
                    ),
            )

        val result =
            registry.resolve(
                DynamicScheduleHistoryEngineVersion("13.10"),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility.MIGRATABLE,
            result.compatibility,
        )

        assertTrue(result.requiresMigration)

        assertFalse(
            result.canReplayDirectly,
            "MIGRATABLE 必须先迁移，不允许直接重放",
        )

        assertEquals(
            "13.10-to-14.0-replay-input-v1",
            result.migrationId,
        )
    }

    // -----------------------------------------------------------------
    // 5. Explicit incompatible
    // -----------------------------------------------------------------

    @Test
    fun registeredIncompatibilityBlocksReplay() {

        val registry =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,

                rules =
                    listOf(
                        IncompatibleVersionRule(
                            historicalVersions = setOf("12.9"),

                            reason =
                                "Priority formula changed between 12.9 and 13.x.",
                        ),
                    ),
            )

        val result =
            registry.resolve(
                DynamicScheduleHistoryEngineVersion("12.9"),
            )

        assertEquals(
            DynamicScheduleHistoryVersionCompatibility
                .INCOMPATIBLE,
            result.compatibility,
        )

        assertTrue(result.isBlocked)

        assertTrue(
            result.reason.contains("Priority formula"),
        )
    }

    // -----------------------------------------------------------------
    // 6. Migration 不修改原始 ReplayInput
    // -----------------------------------------------------------------

    @Test
    fun migrationDoesNotModifyTheOriginalReplayInput() {

        val original =
            DynamicScheduleReplayTestData.create()

        val originalSnapshot =
            original.copy()

        val migration =
            replayInputUpgradeMigration(
                from = "12.27",
                to = "13.10",
            )

        val migrated =
            DynamicScheduleHistoryMigrationRegistry(
                migrations = listOf(migration),
            ).migrate(
                input = original,
                toVersion =
                    DynamicScheduleHistoryEngineVersion("13.10"),
            )

        /*
         * 迁移结果确实换了版本……
         */
        assertEquals(
            "13.10",
            migrated.engineVersion,
        )

        /*
         * ……但原始工件必须一个字段都没变。
         */
        assertEquals(
            originalSnapshot,
            original,
            "迁移不允许修改原始 ReplayInput",
        )

        assertEquals("12.27", original.engineVersion)

        /* 两者并存，而不是覆盖。 */
        assertTrue(
            migrated !== original,
            "迁移必须产出新工件，而不是原地改写",
        )
    }

    @Test
    fun migratingAnUnknownVersionFailsLoudly() {

        val registry =
            DynamicScheduleHistoryMigrationRegistry(
                migrations =
                    listOf(
                        replayInputUpgradeMigration(
                            from = "12.27",
                            to = "13.10",
                        ),
                    ),
            )

        kotlin.test.assertFailsWith<IllegalStateException> {
            registry.migrate(
                input = DynamicScheduleReplayTestData.create(),
                toVersion =
                    DynamicScheduleHistoryEngineVersion("15.0"),
            )
        }
    }

    // -----------------------------------------------------------------
    // 诊断
    // -----------------------------------------------------------------

    @Test
    fun diagnosticExplainsTheDecisionWithoutEnums() {

        val compatibility =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion = current,
            ).resolve(
                DynamicScheduleHistoryEngineVersion("13.1"),
            )

        val diagnostic =
            DynamicScheduleHistoryVersionDiagnostic
                .from(compatibility)

        assertFalse(diagnostic.replayAllowed)

        assertFalse(diagnostic.migrationRequired)

        assertTrue(
            diagnostic.message.contains("13.1"),
        )

        assertTrue(
            diagnostic.message.contains("13.10"),
        )

        assertTrue(
            diagnostic.message.contains("blocked"),
        )
    }

    @Test
    fun migratableDiagnosticSaysBlockedUntilMigration() {

        val compatibility =
            DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
                currentVersion =
                    DynamicScheduleHistoryEngineVersion("14.0"),

                rules =
                    listOf(
                        MigratableVersionRule(
                            historicalVersions = setOf("13.10"),

                            migrationId = "13.10-to-14.0",
                        ),
                    ),
            ).resolve(
                DynamicScheduleHistoryEngineVersion("13.10"),
            )

        val diagnostic =
            DynamicScheduleHistoryVersionDiagnostic
                .from(compatibility)

        assertFalse(diagnostic.replayAllowed)

        assertTrue(diagnostic.migrationRequired)

        assertTrue(
            diagnostic.message
                .contains("blocked until migration"),
        )
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    /**
     * 测试用的最小迁移：只把 engineVersion 换成目标版本。
     *
     * 真实迁移会同时改写 payload 结构，
     * 但这里验证的是"迁移边界不碰原始对象"，
     * 不需要引入真实的格式差异。
     */
    private fun replayInputUpgradeMigration(
        from: String,
        to: String,
    ): DynamicScheduleHistoryMigration =
        object : DynamicScheduleHistoryMigration {

            override val migrationId: String =
                "$from-to-$to-replay-input-v1"

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

    @Test
    fun fixtureSanityCheck() {

        val fixture =
            DynamicScheduleReplayTestData.create()

        assertNotNull(fixture)

        assertEquals("12.27", fixture.engineVersion)
    }
}
