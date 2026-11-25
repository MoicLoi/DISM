package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 15.4 Public Factory Boundary Contract.
 *
 * 这个测试的重点**不是**验证数据库，而是验证：
 *
 * ```text
 * DismFactory.create()
 * ```
 *
 * 一个参数都不需要。
 *
 * 它锁定的是一个很容易被"顺手加回去"的架构性质：
 * 以后不能退化成
 *
 * ```text
 * DismFactory.create(DismPersistenceFactory(...))
 * ```
 *
 * 一旦有人的 PR 把它加回来，这个文件就会编译失败。
 */
class DismFactoryBoundaryTest {

    @Test
    fun publicFactoryCreatesDismWithoutPersistenceArgument() {

        val dism = DismFactory.create()

        assertEquals(
            expected = DismApiVersion.STRING,
            actual = dism.apiVersion,
        )

        assertTrue(dism.capabilities.persistence)

        assertTrue(dism.capabilities.dynamicSchedule)
    }

    @Test
    fun defaultFactoryIsImmediatelyUsable() = runTest {

        val dism = DismFactory.create()

        /*
         * 默认内存 Persistence：空库也应该能安全读快照。
         */
        val snapshot = dism.runtime.snapshot()

        assertTrue(snapshot.entities.isEmpty())

        assertTrue(snapshot.relationStates.isEmpty())

        /*
         * 并且通过**接口**就能拿到调度 Facade。
         */
        val schedule =
            assertNotNull(dism.runtime.dynamicSchedule())

        assertTrue(
            schedule.latestDecision(
                DynamicScheduleItemId("nothing-persisted"),
            ) == null,
        )
    }
}
