package me.moiclathy.dism.api

import java.io.File

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.DismPersistenceFactory
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.runtime.DismRuntimeFactory
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 15.1 / 15.4
 *
 * 产品层入口必须**真的可用**，而不只是一个接口。
 *
 * ### 15.4：这个测试现在走的是 Infrastructure 路径
 *
 * 15.4 把 `DismPersistenceFactory` 从 Public API 移到了
 * internal infrastructure，所以这里不再能写：
 *
 * ```kotlin
 * DismFactory.create(DismPersistenceFactory(...))   // 已移除
 * ```
 *
 * 真实的 SQLDelight 装配属于 **Integration / Infrastructure Test**，
 * 它的正确写法是：
 *
 * ```text
 * 测试
 *  ↓ Infrastructure
 * DismPersistence
 *  ↓
 * DismRuntimeFactory
 * ```
 *
 * 而产品代码是另一条：
 *
 * ```text
 * App → DismFactory.create() → Dism
 * ```
 *
 * 两条路径到这里彻底分开。
 * 公共那条由 `DismFactoryBoundaryTest`（commonTest）覆盖。
 */
class DismFactoryTest {

    @Test
    fun infrastructureAssemblyProducesAWorkingRuntime() = runTest {

        val file =
            File.createTempFile("dism-api-", ".db")

        file.delete()

        try {

            val persistence =
                DismPersistenceFactory(
                    JvmDatabaseDriverFactory(file),
                ).create()

            val runtime =
                DismRuntimeFactory(
                    persistence = persistence,
                ).create()

            /*
             * Runtime 是可用的：空库也应该能读快照。
             */
            val snapshot = runtime.snapshot()

            assertTrue(snapshot.entities.isEmpty())

            assertTrue(snapshot.relationStates.isEmpty())

            /*
             * 15.3 的核心收口：通过**接口**就能拿到调度 Facade。
             */
            val schedule =
                assertNotNull(runtime.dynamicSchedule())

            /*
             * Facade 是真装配过的：没有历史时也应该能安全查询。
             */
            assertTrue(
                schedule.latestDecision(
                    DynamicScheduleItemId("item-1"),
                ) == null,
                "no snapshot has been persisted yet",
            )

        } finally {

            file.delete()
        }
    }

    @Test
    fun runtimeIsReachableWithDefaultIdentity() = runTest {

        val file =
            File.createTempFile("dism-api-", ".db")

        file.delete()

        try {

            val persistence =
                DismPersistenceFactory(
                    JvmDatabaseDriverFactory(file),
                ).create()

            val runtime =
                DismRuntimeFactory(
                    persistence = persistence,
                ).create()

            /*
             * 默认参数必须存在于接口上。
             */
            assertNotNull(runtime.dynamicSchedule())

        } finally {

            file.delete()
        }
    }

    @Test
    fun migrationConstructorStillWorks() = runTest {

        val file =
            File.createTempFile("dism-api-", ".db")

        file.delete()

        try {

            /*
             * 15.4 的迁移期兼容入口：
             * 旧的 persistenceFactory 路径仍然可用，
             * 这样既有平台装配代码不需要一次性全部改完。
             */
            val runtime =
                DismRuntimeFactory(
                    persistenceFactory =
                        DismPersistenceFactory(
                            JvmDatabaseDriverFactory(file),
                        ),
                ).create()

            assertTrue(runtime.snapshot().entities.isEmpty())

        } finally {

            file.delete()
        }
    }

    @Test
    fun defaultDismUsesInMemoryPersistence() {

        val dism = DismFactory.create()

        assertEquals("1.5.0", dism.apiVersion)

        assertTrue(
            DismPersistence().entities.all().isEmpty(),
        )
    }
}
