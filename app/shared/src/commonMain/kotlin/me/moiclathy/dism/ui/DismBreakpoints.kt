package me.moiclathy.dism.ui

/**
 * 16.33：窗口尺寸等级。
 *
 * 取值与 Material 3 的 window size class 对齐：
 *
 * ```text
 * COMPACT    < 600dp    手机竖屏、窄窗口
 * MEDIUM     600–839dp  平板竖屏、桌面窄窗口
 * EXPANDED   >= 840dp   平板横屏、桌面常规窗口
 * ```
 *
 * 用 Material 的档位而不是自己定义的阈值，理由是
 * **它不是随意的数字**：这些边界来自大量设备实测，
 * 而且与 Compose 生态里其它组件的预期一致。
 */
enum class DismWindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

/**
 * 16.33：断点计算。
 *
 * ### 为什么把它做成纯函数
 *
 * 之前这个判断是内联的：
 *
 * ```kotlin
 * val wide = maxWidth >= 600.dp
 * ```
 *
 * 一行 `>=` 看起来不值得抽出来。但它有两个问题：
 *
 * ```text
 * 1. 它是**产品规则**（"多宽算宽"），埋在布局代码里
 *    没人会去检查它是否仍然合理
 * 2. 它不可测——而 Composable 里的 `if` 分支
 *    只能靠 UI 测试覆盖，那需要一整套新基建
 * ```
 *
 * 抽成 `(Int) -> DismWindowSize` 之后，边界值可以逐个断言。
 * 断点最容易出错的地方恰恰是**边界**：
 * 600 到底属于窄还是宽？
 *
 * ### 为什么收 `Int` 而不是 `Dp`
 *
 * `Dp` 是 Compose 类型。收 `Int`（dp 的整数值）让这个函数
 * 不需要任何 Compose 依赖，从而能在 commonTest 里直接跑。
 *
 * 调用方做一次 `.value.toInt()` 转换——那是唯一需要
 * 接触 Compose 的地方。
 */
object DismBreakpoints {

    /**
     * MEDIUM 的下界（含）。
     */
    const val MEDIUM_MIN_WIDTH_DP = 600

    /**
     * EXPANDED 的下界（含）。
     */
    const val EXPANDED_MIN_WIDTH_DP = 840

    fun windowSizeFor(
        widthDp: Int,
    ): DismWindowSize =
        when {
            widthDp < MEDIUM_MIN_WIDTH_DP ->
                DismWindowSize.COMPACT

            widthDp < EXPANDED_MIN_WIDTH_DP ->
                DismWindowSize.MEDIUM

            else ->
                DismWindowSize.EXPANDED
        }
}
