package me.moiclathy.dism.platform

import java.io.File
import java.nio.file.Files

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

import kotlin.time.Instant

import kotlinx.coroutines.runBlocking

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.config.DismStorageMode
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 16.14：平台落盘必须**真的**跨进程/跨实例存活。
 *
 * ```text
 * 只看"create 没抛错"是不够的：
 * 一个永远写不进去的 provider 也能让 create 成功。
 *
 * 真正的证明是：
 *   实例 A 写 → 销毁 → 实例 B 从同一个文件读出来
 * ```
 */
class JvmDismFactoryTest {

    private val scheduleId = DynamicScheduleId("persist-schedule")

    private val databaseFile: File =
        Files.createTempDirectory("dism-persistence-test")
            .resolve("dism.db")
            .toFile()

    @AfterTest
    fun cleanUp() {
        databaseFile.parentFile
            ?.deleteRecursively()
    }

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    private fun model() =
        DynamicScheduleModel(
            id = scheduleId,
            revision = 1L,
            items =
                listOf(
                    DynamicScheduleItem(
                        id = DynamicScheduleItemId("persisted"),
                        scheduleId = scheduleId,
                        title = "Persisted item",
                        type = DynamicScheduleItemType.EVENT,
                        source = DynamicScheduleSource.USER_INPUT,
                        temporal =
                            DynamicScheduleTemporal.At(now),
                        state = DynamicScheduleState.UPCOMING,
                    ),
                ),
        )

    @Test
    fun decisionSurvivesACloseAndReopen() = runBlocking {

        /*
         * 实例 A：评估一次，这会写入 Snapshot / Trace / ReplayInput。
         */
        val first =
            JvmDismFactory.create(databaseFile = databaseFile)

        first.runtime
            .dynamicSchedule()
            .evaluate(model = model(), now = now)

        /*
         * 实例 B：全新的 Dism，同一个文件。
         */
        val second =
            JvmDismFactory.create(databaseFile = databaseFile)

        val history =
            second.runtime
                .dynamicSchedule()
                .history
                .query(
                    itemId =
                        DynamicScheduleItemId("persisted"),
                )

        assertTrue(
            databaseFile.exists(),
            "the database file must actually be written",
        )

        assertEquals(
            1,
            history.totalMatched,
            "the decision recorded by the first instance " +
                "must be readable from a new instance",
        )

        assertEquals(
            "persisted",
            history.entries.first().itemId.value,
        )
    }

    @Test
    fun separateFilesDoNotShareState() = runBlocking {

        val otherFile =
            File(
                databaseFile.parentFile,
                "other.db",
            )

        val a =
            JvmDismFactory.create(databaseFile = databaseFile)

        val b =
            JvmDismFactory.create(databaseFile = otherFile)

        a.runtime
            .dynamicSchedule()
            .evaluate(model = model(), now = now)

        val fromB =
            b.runtime
                .dynamicSchedule()
                .history
                .query(
                    itemId =
                        DynamicScheduleItemId("persisted"),
                )

        /*
         * 反证：如果 provider 忽略了传入的文件、而总是写同一个
         * 位置（例如都落到默认路径），这条会失败。
         *
         * 它是必要的，因为"能读出来"本身并不能证明
         * 数据是被写到**这个**文件里的。
         */
        assertEquals(
            0,
            fromB.totalMatched,
            "a second database file must start empty",
        )

        assertTrue(otherFile.exists())
    }

    @Test
    fun localModeIsForcedRegardlessOfThePassedConfiguration() =
        runBlocking {

            /*
             * 传一个 IN_MEMORY 配置进来。
             *
             * JvmDismFactory 的语义是"我要落盘"，
             * 所以它必须**纠正**配置，而不是静默退化成内存——
             * 后者会让用户以为存下来了，重启后什么都没有。
             */
            val dism =
                JvmDismFactory.create(
                    databaseFile = databaseFile,
                    configuration =
                        DismConfiguration(
                            storage =
                                DismStorageConfiguration(
                                    mode =
                                        DismStorageMode.IN_MEMORY,
                                    databaseName = "dism",
                                ),
                        ),
                )

            dism.runtime
                .dynamicSchedule()
                .evaluate(model = model(), now = now)

            val reopened =
                JvmDismFactory.create(
                    databaseFile = databaseFile,
                )

            val history =
                reopened.runtime
                    .dynamicSchedule()
                    .history
                    .query(
                        itemId =
                            DynamicScheduleItemId("persisted"),
                    )

            assertEquals(
                1,
                history.totalMatched,
                "passing IN_MEMORY to a file-backed factory " +
                    "must not silently disable persistence",
            )
        }

    @Test
    fun parentDirectoriesAreCreated() = runBlocking {

        val nested =
            File(
                databaseFile.parentFile,
                "a/b/c/nested.db",
            )

        /*
         * 用户给的路径里带不存在的目录是很常见的
         * （例如首次运行时 `~/.dism/` 还没建）。
         * 直接失败会让"第一次使用"变成一次崩溃。
         */
        assertTrue(
            !nested.parentFile.exists(),
            "precondition: the parent directory must not exist",
        )

        JvmDismFactory.create(databaseFile = nested)

        assertTrue(
            nested.parentFile.exists(),
            "the factory must create missing parent directories",
        )
    }

    @Test
    fun defaultConfigurationIsNotLocal() {

        /*
         * JvmDismFactory 强制 LOCAL，但这**不应该**改变
         * DismFactory 的默认值。
         *
         * 默认内存是有意的：它让 `DismFactory.create()` 在
         * 任何平台、任何测试里都能工作，不产生外部副作用。
         */
        assertNotEquals(
            DismStorageMode.LOCAL,
            DismConfiguration().storage.mode,
        )
    }
}

/**
 * 一个容易忽略的编译期事实：`Dism` 是公共类型，
 * 所以它可以从应用模块持有。
 */
private fun holdsPublicType(
    dism: Dism,
): Dism = dism
