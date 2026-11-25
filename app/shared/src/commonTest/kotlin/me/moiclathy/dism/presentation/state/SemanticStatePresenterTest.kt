package me.moiclathy.dism.presentation.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

import me.moiclathy.dism.api.DismFactory

/**
 * 16.31：库级状态的按需加载。
 *
 * ### 这里最容易被做错的是什么
 *
 * 不是"读到了多少"，而是**"还没读"与"读到了空的"被混为一谈**。
 *
 * ```text
 * isLoaded = false   还没读过          →  正在读取…
 * isLoaded = true，0 条   读过了，确实是空的 →  DISM 还什么都不知道
 * ```
 *
 * 这两种状态在界面上必须看起来不一样。把它们合并的话，
 * 用户打开这一页会先看到"DISM 什么都还不知道"——
 * 而那是**还没去读**，不是事实。
 *
 * 注意这里只能用空库（`DismFactory.create()` 是内存实现）。
 * 计数是否随库大小变化由 core 的
 * `RuntimeSnapshotCostTest` 覆盖。
 */
class SemanticStatePresenterTest {

    private fun presenter() =
        SemanticStatePresenter(DismFactory.create())

    @Test
    fun aFreshStateIsNotLoaded() {

        /*
         * `App` 的初始状态就是这个。
         *
         * 它对应界面上短暂的"正在读取…"，
         * 而不是"DISM 什么都不知道"。
         */
        val fresh = SemanticStateUiState()

        assertTrue(!fresh.isLoaded)

        assertNull(fresh.errorMessage)
    }

    @Test
    fun loadingMarksTheStateAsLoaded() {

        val loaded = presenter().load()

        assertEquals(
            true,
            loaded.isLoaded,
            "a completed read must be marked as loaded, " +
                "otherwise the screen keeps saying " +
                "'reading…' forever",
        )

        assertNull(loaded.errorMessage)
    }

    @Test
    fun anEmptyLibraryIsLoadedButEmpty() {

        val loaded = presenter().load()

        /*
         * 这一条是整组测试的重点：
         *
         * ```text
         * isLoaded = true   AND   isEmpty = true
         * ```
         *
         * 两个都为真才是"读过了，确实没有"。
         * 少了 isLoaded，界面无法把它与"还没读"区分开。
         */
        assertTrue(loaded.isLoaded)

        assertTrue(loaded.isEmpty)

        assertEquals(0, loaded.entityCount)

        assertEquals(0, loaded.relationCount)

        assertEquals(0, loaded.activeRelationCount)
    }

    @Test
    fun theProjectionAlwaysMarksItselfLoaded() {

        /*
         * 投影函数是"已经拿到快照之后"的纯映射，
         * 所以它产出的东西**必然**是已加载的。
         *
         * 如果哪天有人在 `semanticStateUiState` 里
         * 忘了置 `isLoaded = true`，界面会永远停在
         * "正在读取…"，而这是静态可测的。
         */
        val projected =
            semanticStateUiState(
                snapshot =
                    DismFactory.create()
                        .runtime
                        .snapshot(),
            )

        assertTrue(projected.isLoaded)

        assertTrue(projected.isEmpty)
    }
}
