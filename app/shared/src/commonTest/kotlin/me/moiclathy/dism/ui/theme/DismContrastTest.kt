package me.moiclathy.dism.ui.theme

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * 16.30：配色的对比度门禁。
 *
 * 这些断言的意义在于：配色是手挑的，而手挑的东西
 * 会在一次"让它好看一点"的改动里静默退化。
 *
 * ### 颜色在这里是**重复的常量**
 *
 * 主题里的颜色是 `Color(...)`，它的通道值在
 * `androidx.compose.ui.graphics` 里；而这个测试跑在 commonTest，
 * 拿到的是同一个类型。所以下面的常量与 `DismTheme.kt`
 * **必须**保持一致。
 *
 * 这是当前设计的弱点：两处常量会漂移。
 * `colorsMatchTheThemeSource` 用源码文本比对来兜住它——
 * 见那个测试的说明。
 */
class DismContrastTest {

/*
     * 颜色**不在这里重复定义**，全部取自 DismPalette。
     *
     * 如果这里再写一遍数字，就会出现两份会漂移的常量：
     * 有人调了主题、门禁却还在查旧颜色。
     * 现在这种漂移在结构上不可能发生。
     */

    // ── 浅色主题 ────────────────────────────────────────

    private val lightOnSurface = DismPalette.LIGHT_ON_SURFACE
    private val lightSurface = DismPalette.LIGHT_SURFACE
    private val lightSurfaceVariant =
        DismPalette.LIGHT_SURFACE_VARIANT
    private val lightOnSurfaceVariant =
        DismPalette.LIGHT_ON_SURFACE_VARIANT
    private val lightPrimary = DismPalette.LIGHT_PRIMARY
    private val lightOnPrimary = DismPalette.LIGHT_ON_PRIMARY
    private val lightPrimaryContainer =
        DismPalette.LIGHT_PRIMARY_CONTAINER
    private val lightOnPrimaryContainer =
        DismPalette.LIGHT_ON_PRIMARY_CONTAINER
    private val lightError = DismPalette.LIGHT_ERROR
    private val lightOnError = DismPalette.LIGHT_ON_ERROR

    // ── 深色主题 ────────────────────────────────────────

    private val darkOnSurface = DismPalette.DARK_ON_SURFACE
    private val darkSurface = DismPalette.DARK_SURFACE
    private val darkSurfaceVariant =
        DismPalette.DARK_SURFACE_VARIANT
    private val darkOnSurfaceVariant =
        DismPalette.DARK_ON_SURFACE_VARIANT
    private val darkPrimary = DismPalette.DARK_PRIMARY
    private val darkPrimaryContainer =
        DismPalette.DARK_PRIMARY_CONTAINER
    private val darkOnPrimaryContainer =
        DismPalette.DARK_ON_PRIMARY_CONTAINER
    private val darkError = DismPalette.DARK_ERROR
    private val darkOnError = DismPalette.DARK_ON_ERROR

    @Test
    fun bodyTextMeetsAaInBothThemes() {

        /*
         * 正文用的是 onSurface / onBackground，背景是 surface。
         * 这是最常出现的一对，也最不能失败。
         */
        assertRatio(
            label = "light onSurface / surface",
            foreground = lightOnSurface,
            background = lightSurface,
        )

        assertRatio(
            label = "dark onSurface / surface",
            foreground = darkOnSurface,
            background = darkSurface,
        )
    }

    @Test
    fun secondaryTextMeetsAaInBothThemes() {

        /*
         * 时间标签、副标题、计数说明都走这一对。
         *
         * 它是最容易失败的：设计上想让次要信息"淡一点"，
         * 而"淡一点"很容易掉到 4.5 以下。
         */
        assertRatio(
            label = "light onSurfaceVariant / surface",
            foreground = lightOnSurfaceVariant,
            background = lightSurface,
        )

        assertRatio(
            label = "dark onSurfaceVariant / surface",
            foreground = darkOnSurfaceVariant,
            background = darkSurface,
        )

        /*
         * 次要文字也会出现在 surfaceVariant 上
         * （例如 History 的卡片）。
         */
        assertRatio(
            label = "light onSurfaceVariant / surfaceVariant",
            foreground = lightOnSurfaceVariant,
            background = lightSurfaceVariant,
        )

        assertRatio(
            label = "dark onSurfaceVariant / surfaceVariant",
            foreground = darkOnSurfaceVariant,
            background = darkSurfaceVariant,
        )
    }

    @Test
    fun accentTextMeetsAaOnItsContainer() {

        /*
         * Attention 卡片与 Import 反馈横幅用的是
         * primaryContainer + onPrimaryContainer。
         */
        assertRatio(
            label = "light onPrimaryContainer / primaryContainer",
            foreground = lightOnPrimaryContainer,
            background = lightPrimaryContainer,
        )

        assertRatio(
            label = "dark onPrimaryContainer / primaryContainer",
            foreground = darkOnPrimaryContainer,
            background = darkPrimaryContainer,
        )
    }

    @Test
    fun errorTextMeetsAa() {

        assertRatio(
            label = "light error / surface",
            foreground = lightError,
            background = lightSurface,
        )

        assertRatio(
            label = "dark error / surface",
            foreground = darkError,
            background = darkSurface,
        )

        /*
         * 深色主题下 onError 只用于 error 容器上的文字。
         */
        assertRatio(
            label = "light onError",
            foreground = lightOnError,
            background = lightError,
        )
    }

    @Test
    fun accentIsDistinguishableFromTheBackground() {

        /*
         * primary 被用在：时间标签的高亮、TimelineRow 的强调色、
         * 以及需要与背景区分的界面元素。
         *
         * 这属于"界面组件"，AA 要求 3:1 而不是 4.5。
         */
        assertRatio(
            label = "light primary / surface (component)",
            foreground = lightPrimary,
            background = lightSurface,
            minimum = DismContrast.AA_LARGE_TEXT_OR_COMPONENT,
        )

        assertRatio(
            label = "dark primary / surface (component)",
            foreground = darkPrimary,
            background = darkSurface,
            minimum = DismContrast.AA_LARGE_TEXT_OR_COMPONENT,
        )
    }

    @Test
    fun theRatioFunctionFollowsWcag() {

        /*
         * 先验证函数本身，再验证配色。
         *
         * 否则一个算错的对比度函数会让上面所有断言都失去意义——
         * 而且它会"全部通过"。
         */
        assertEquals(
            21.0,
            DismContrast.ratio(0xFFFFFFFF, 0xFF000000),
            absoluteTolerance = 0.01,
            message = "black on white is the maximum, 21:1",
        )

        assertEquals(
            1.0,
            DismContrast.ratio(0xFF3B5BDB, 0xFF3B5BDB),
            absoluteTolerance = 0.001,
            message = "a colour against itself is 1:1",
        )

        /*
         * 顺序无关。
         */
        assertEquals(
            DismContrast.ratio(0xFFFFFFFF, 0xFF000000),
            DismContrast.ratio(0xFF000000, 0xFFFFFFFF),
            absoluteTolerance = 0.001,
        )

        /*
         * 中灰对白：WCAG 的经典校验点。
         *
         * 用**线性** RGB 直算会得到约 3.9，
         * 正确值约 3.95…… 为了不把这个测试做成
         * "把实现抄一遍"，这里只断言一个区间。
         */
        val grey = DismContrast.ratio(0xFF808080, 0xFFFFFFFF)

        assertTrue(
            abs(grey - 3.95) < 0.15,
            "grey/white should be about 3.95, was $grey",
        )
    }

    private fun assertRatio(
        label: String,
        foreground: Long,
        background: Long,
        minimum: Double = DismContrast.AA_NORMAL_TEXT,
    ) {
        val ratio = DismContrast.ratio(foreground, background)

        assertTrue(
            ratio >= minimum,
            "$label has contrast " +
                "${(ratio * 100).toInt() / 100.0} : 1, " +
                "which is below the required $minimum : 1",
        )
    }
}
