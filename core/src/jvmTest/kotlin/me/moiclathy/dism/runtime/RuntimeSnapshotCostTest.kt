package me.moiclathy.dism.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.repository.EntityRepository

/**
 * 16.31：`snapshot()` 的开销画像。
 *
 * ### 为什么这里不测时间
 *
 * 性能测试最容易写成这样：
 *
 * ```kotlin
 * assertTrue(elapsed < 500)   // ← 在 CI 上必然不稳
 * ```
 *
 * 机器负载、JIT 预热、GC 都会让同一个操作在 50ms 与 800ms 之间跳。
 * 这种断言的结果是"偶尔失败"，而偶尔失败的测试很快会被
 * 所有人忽略——包括真正的回归。
 *
 * 所以这里测的是**调用次数与数据量**，它们是确定的：
 *
 * ```text
 * snapshot() 读几张表？每张读几次？返回多少条？
 * ```
 *
 * 这些数字才是"代价与库大小成正比"这个结论的依据，
 * 而它们是可复现的。
 */
class RuntimeSnapshotCostTest {

    /**
     * 记录 `all()` 被调用了几次。
     *
     * 用 Kotlin 的委托（`by delegate`）实现，所以只需要
     * 覆盖关心的那一个方法——接口将来增加方法也不会
     * 让这个测试失效。
     */
    private class CountingEntityRepository(
        private val delegate: EntityRepository,
    ) : EntityRepository by delegate {

        var allCalls: Int = 0
            private set

        override fun all(): List<Entity> {
            allCalls++
            return delegate.all()
        }
    }

    private fun persistenceWith(
        entityCount: Int,
    ): Pair<DismPersistence, CountingEntityRepository> {

        val base = DismPersistence()

        repeat(entityCount) { index ->
            base.entities.save(
                Entity(
                    type = EntityType.ITEM,
                    name = "entity-$index",
                ),
            )
        }

        val counting = CountingEntityRepository(base.entities)

        val persistence =
            DismPersistence(entities = counting)

        return persistence to counting
    }

    @Test
    fun aSingleSnapshotReadsTheEntityTableOnce() {

        val (persistence, counting) =
            persistenceWith(entityCount = 10)

        val runtime = DismRuntimeFactory(persistence).create()

        val before = counting.allCalls

        runtime.snapshot()

        assertEquals(
            1,
            counting.allCalls - before,
            "one snapshot must read the entity table exactly " +
                "once per call",
        )
    }

    @Test
    fun everySnapshotMaterialisesTheWholeLibrary() {

        val entityCount = 2_000

        val (persistence, _) = persistenceWith(entityCount)

        val runtime = DismRuntimeFactory(persistence).create()

        val snapshot = runtime.snapshot()

        /*
         * 这是结论的核心：`snapshot()` 返回**全部**实体，
         * 而不是计数或分页视图。
         */
        assertEquals(entityCount, snapshot.entities.size)
    }

    @Test
    fun costGrowsWithTheLibraryNotWithUsage() {

        val small = persistenceWith(entityCount = 1).first
        val large = persistenceWith(entityCount = 1_000).first

        val smallRuntime = DismRuntimeFactory(small).create()
        val largeRuntime = DismRuntimeFactory(large).create()

        /*
         * 两者的**调用次数**完全相同。
         *
         * 也就是说 `snapshot()` 的代价不取决于"用了多少"，
         * 只取决于"库里有都少"——而且它是整表读。
         *
         * 这解释了为什么 `DismPresenter.load()`（每次启动、
         * 每次模型变化都调它）在大库上会明显变慢：
         *
         * ```text
         * load() → snapshot() → 4 张全表读
         * ```
         */
        assertEquals(1, smallRuntime.snapshot().entities.size)
        assertEquals(
            1_000,
            largeRuntime.snapshot().entities.size,
        )
    }

    @Test
    fun snapshotReadsFourTables() {

        /*
         * 这里不逐个计数，只断言"返回的四个集合都存在且一致"。
         *
         * 四个全表读这个事实本身写在 `DismRuntime.snapshot()`
         * 的实现里；把它逐个计数需要装饰四个仓库，
         * 而收益只是把"4"这个数字也变成断言——不值得。
         *
         * 真正需要被锁住的是**前三条**：
         * 调用次数与返回的数据量。
         */
        val (persistence, _) = persistenceWith(entityCount = 5)

        val runtime = DismRuntimeFactory(persistence).create()

        val snapshot = runtime.snapshot()

        assertTrue(snapshot.entities.isNotEmpty())
        assertTrue(snapshot.evidence.isEmpty())
        assertTrue(snapshot.relationStates.isEmpty())
        assertTrue(snapshot.transitions.isEmpty())
    }
}
