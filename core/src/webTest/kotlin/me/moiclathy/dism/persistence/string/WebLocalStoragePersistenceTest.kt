package me.moiclathy.dism.persistence.string

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.platform.WebDismFactory
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 16.34：**在真实浏览器里**验证 localStorage 绑定与 Web 装配。
 *
 * ### 为什么这一组测试值得存在
 *
 * `JsonSnapshotStoreTest` 已经在浏览器里跑过（它属于 commonTest），
 * 但它用的是**内存** `StringStorage`——验证的是存储语义，
 * 不是"浏览器里真的能存下来"。
 *
 * 差的那一段恰好是用户唯一能感知的部分：
 *
 * ```text
 * 刷新页面  →  应用重建  →  历史还在吗？
 * ```
 *
 * 所以这里的测试直接模拟"刷新"：
 *
 * ```text
 * 用 WebDismFactory 建一个 Dism，写一条决策
 * 丢掉它
 * 再建一个        ← 等价于刷新页面
 * 看历史还在不在
 * ```
 *
 * ### 它只能跑在浏览器目标上
 *
 * `LocalStorageStringStorage` 在 `webMain`，所以这些测试
 * 只在 `jsBrowserTest` 与 `wasmJsBrowserTest` 里执行。
 * 在 JVM 上它们不会被编译进去——也不需要，
 * JVM 没有 `localStorage`。
 */
class WebLocalStoragePersistenceTest {

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    /**
     * 每个测试从干净状态开始。
     *
     * 浏览器测试跑在同一个页面里，`localStorage` 在用例之间
     * **共享**——不清的话用例顺序会影响结果，
     * 而那种失败极难复现。
     */
    private fun clearBrowserStorage() {
        val storage = LocalStorageStringStorage()

        storage.remove("dism.schedule.decision.snapshots")
        storage.remove("probe.key")
    }

    private fun model() =
        DynamicScheduleModel(
            id = DynamicScheduleId("web-test"),
            revision = 1L,
            items =
                listOf(
                    DynamicScheduleItem(
                        id = DynamicScheduleItemId("web-item"),
                        scheduleId = DynamicScheduleId("web-test"),
                        title = "Browser test item",
                        type = DynamicScheduleItemType.EVENT,
                        source = DynamicScheduleSource.USER_INPUT,
                        temporal =
                            DynamicScheduleTemporal.At(now),
                        state = DynamicScheduleState.UPCOMING,
                    ),
                ),
        )

    // ── 绑定本身 ────────────────────────────────────────

    @Test
    fun localStorageStorageRoundTripsInTheBrowser() {

        clearBrowserStorage()

        val storage = LocalStorageStringStorage()

        assertNull(storage.get("probe.key"))

        storage.set("probe.key", "hello from the browser")

        /*
         * 用一个**新的实例**读，证明值真的落到了
         * 浏览器的 localStorage 里，而不是某个对象的字段。
         */
        assertEquals(
            "hello from the browser",
            LocalStorageStringStorage().get("probe.key"),
        )

        storage.remove("probe.key")

        assertNull(storage.get("probe.key"))
    }

    // ── 用户能感知的那一条承诺 ───────────────────────────

    @Test
    fun historySurvivesANewDismInstance() = runTest {

        clearBrowserStorage()

        val itemId = DynamicScheduleItemId("web-item")

        /*
         * 第一个实例：写一条决策历史。
         */
        val first = WebDismFactory.create()

        first.runtime
            .dynamicSchedule()
            .evaluate(model = model(), now = now)

        /*
         * 丢掉它，重建一个——这正是"刷新页面"做的事。
         */
        val second = WebDismFactory.create()

        val history =
            second.runtime
                .dynamicSchedule()
                .history
                .query(itemId = itemId)

        assertEquals(
            1,
            history.totalMatched,
            "history written by one instance must be readable " +
                "by a newly created one; this is what makes " +
                "a page refresh non-destructive",
        )

        assertEquals(itemId, history.entries.first().itemId)
    }

    @Test
    fun theBatchLookupSeesPersistedHistory() = runTest {

        clearBrowserStorage()

        val itemId = DynamicScheduleItemId("web-item")

        WebDismFactory.create()
            .runtime
            .dynamicSchedule()
            .evaluate(model = model(), now = now)

        /*
         * 16.32 的批量入口也走同一条 localStorage 通道。
         *
         * 它不与单条查询共用代码路径（一个走 JSON 过滤，
         * 一个走 SQL），所以值得单独确认。
         */
        val batch =
            WebDismFactory.create()
                .runtime
                .dynamicSchedule()
                .latestDecisions(listOf(itemId))

        assertTrue(
            batch.containsKey(itemId),
            "the batch lookup must see history persisted by a " +
                "previous instance; keys=${batch.keys}",
        )

        assertEquals(
            "Browser test item",
            batch.getValue(itemId).title,
        )
    }

    @Test
    fun anEmptyBrowserStorageStartsClean() = runTest {

        clearBrowserStorage()

        /*
         * 干净浏览器里的第一个实例不该凭空看到历史。
         *
         * 没有这一条的话，上面的测试可能因为
         * "上一个用例留下的数据"而通过——
         * 那是假通过。
         */
        val history =
            WebDismFactory.create()
                .runtime
                .dynamicSchedule()
                .history
                .query(
                    itemId = DynamicScheduleItemId("web-item"),
                )

        assertEquals(0, history.totalMatched)
    }
}
