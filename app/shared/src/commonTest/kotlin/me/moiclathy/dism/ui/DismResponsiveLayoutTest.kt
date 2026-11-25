package me.moiclathy.dism.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.presentation.navigation.DismDestination

/**
 * 16.33：响应式规则。
 *
 * 断点与导航构成的规则本来是内联在 Composable 里的两个
 * `if` / `filter`，只有 UI 测试才能覆盖——而那需要一整套新基建。
 * 抽成纯函数之后，边界值可以逐个断言。
 */
class DismResponsiveLayoutTest {

    // ── 断点 ────────────────────────────────────────────

    @Test
    fun theBoundariesThemselvesAreAsserted() {

        /*
         * 边界是断点最容易出错的地方：
         * 600 到底属于窄还是宽？
         *
         * 这条测试把答案写死：600 是 **MEDIUM 的下界**（含），
         * 599 仍然属于 COMPACT。
         */
        assertEquals(
            DismWindowSize.COMPACT,
            DismBreakpoints.windowSizeFor(599),
        )

        assertEquals(
            DismWindowSize.MEDIUM,
            DismBreakpoints.windowSizeFor(600),
        )

        assertEquals(
            DismWindowSize.MEDIUM,
            DismBreakpoints.windowSizeFor(839),
        )

        assertEquals(
            DismWindowSize.EXPANDED,
            DismBreakpoints.windowSizeFor(840),
        )
    }

    @Test
    fun extremeWidthsDoNotThrow() {

        /*
         * 宽度为 0（测量前的初始帧）与极宽都不该崩。
         *
         * `BoxWithConstraints` 在首帧可能报出 0——
         * 那时如果分支里有除零或负数处理不当就会出问题。
         */
        assertEquals(
            DismWindowSize.COMPACT,
            DismBreakpoints.windowSizeFor(0),
        )

        assertEquals(
            DismWindowSize.EXPANDED,
            DismBreakpoints.windowSizeFor(Int.MAX_VALUE),
        )
    }

    @Test
    fun theThresholdsAreOrdered() {

        /*
         * 两个阈值必须递增。
         *
         * 写反的话 `windowSizeFor` 会永远返回同一个值，
         * 而上面的边界断言会立刻失败——但那时的错误信息
         * （"599 不是 COMPACT"）指向的是症状，不是原因。
         * 这一条直接把原因说清楚。
         */
        assertTrue(
            DismBreakpoints.MEDIUM_MIN_WIDTH_DP <
                DismBreakpoints.EXPANDED_MIN_WIDTH_DP,
        )
    }

    // ── 导航规则 ────────────────────────────────────────

    @Test
    fun compactShowsOnlyPrimaryDestinations() {

        val shown = navDestinationsFor(DismWindowSize.COMPACT)

        assertTrue(shown.isNotEmpty())

        assertTrue(
            shown.all { it.primary },
            "a phone must not list drill-down destinations; " +
                "was=$shown",
        )
    }

    @Test
    fun widerSizesAddTheDrillDownDestinations() {

        val compact = navDestinationsFor(DismWindowSize.COMPACT)
        val medium = navDestinationsFor(DismWindowSize.MEDIUM)
        val expanded =
            navDestinationsFor(DismWindowSize.EXPANDED)

        assertTrue(
            medium.size > compact.size,
            "MEDIUM should expose the drill-down views",
        )

        /*
         * COMPACT 下的每一项在 MEDIUM 下仍然在——
         * 变宽只应该**增加**入口，不应该让某个入口消失。
         *
         * 那会让用户在拉宽窗口时"丢失"一个页面。
         */
        assertTrue(
            medium.containsAll(compact),
            "widening must not remove entries",
        )

        assertEquals(medium, expanded)
    }

    @Test
    fun unfinishedDestinationsAreNeverListed() {

        /*
         * 与"没接线的平台不显示 Import 入口"是同一条原则：
         * 一个点进去说"敬请期待"的入口比没有入口更糟。
         *
         * DIAGNOSTICS 是当前唯一还没页面的目的地。
         */
        DismWindowSize.entries.forEach { size ->
            assertTrue(
                DismDestination.DIAGNOSTICS !in
                    navDestinationsFor(size),
                "an unimplemented screen must not be " +
                    "listed at $size",
            )
        }

/*
         * MEMORY 是 `primary = true` 但 `implemented = false`——
         * 也就是说"它应该占据一个主位置，但页面还没做"。
         *
         * 这两件事分开表达正是 16.33 的要点：
         *
         * ```text
         * primary     它值不值得占一个主位置
         * implemented 现在能不能打开
         * ```
         *
         * 混为一谈时，要么漏列（页面做好却忘了加），
         * 要么列了打不开（MEMORY 之前就是这个状态）。
         */
        assertTrue(DismDestination.MEMORY.primary)

        assertTrue(!DismDestination.MEMORY.implemented)
    }

    @Test
    fun everyListedDestinationHasAPrimaryOrDesktopRole() {

        /*
         * 穷举检查：列出来的每一项都必须有明确的"为什么在这里"。
         *
         * 新增目的地时，`when`/`filter` 不会报错——
         * 它会默认被排除。这条断言把默认行为说清楚。
         */
        DismWindowSize.entries.forEach { size ->
            navDestinationsFor(size).forEach { destination ->
                assertTrue(
                    destination.primary ||
                        destination.desktopOnly,
                    "$destination is neither primary nor " +
                        "desktopOnly but was listed at $size",
                )
            }
        }
    }

    // ── 内容宽度 ────────────────────────────────────────

    @Test
    fun contentWidthIsCappedAtTheExpandedBreakpoint() {

        /*
         * 行太长会让人在换行时丢行。上限与 EXPANDED 的断点
         * 一致，而不是另取一个数字——"什么时候算宽"与
         * "内容最多多宽"用同一个数量级更容易理解。
         */
        assertEquals(
            DismBreakpoints.EXPANDED_MIN_WIDTH_DP,
            MAX_CONTENT_WIDTH_DP,
        )

        /*
         * 上限必须**大于** COMPACT 的上界，否则手机上的内容
         * 会比屏幕还窄——那是个荒唐的结果，但很容易在
         * 调数字时发生。
         */
        assertTrue(
            MAX_CONTENT_WIDTH_DP >
                DismBreakpoints.MEDIUM_MIN_WIDTH_DP,
        )
    }
}
