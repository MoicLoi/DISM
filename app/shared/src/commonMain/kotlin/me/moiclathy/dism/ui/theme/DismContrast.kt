package me.moiclathy.dism.ui.theme

import kotlin.math.pow

/**
 * 16.30：对比度计算（WCAG 2.1）。
 *
 * ### 为什么这是一个可测的门禁，而不是一次人工检查
 *
 * 配色是**手挑**的。手挑的东西最容易在改动时静默退化：
 *
 * ```text
 * 有人为了让某个按钮好看一点，把 primary 调亮了
 * → 正文对比度从 4.6 掉到 4.2
 * → 低于 WCAG AA 的 4.5
 * → 没有任何人会发现
 * ```
 *
 * 而对比度是**纯数学**，可以写成断言。
 * 于是"配色改了但没检查对比度"这件事就不会发生。
 *
 * ### 阈值（WCAG 2.1 AA）
 *
 * ```text
 * 正文文字（小于 18pt / 14pt 粗体）   ≥ 4.5 : 1
 * 大字（≥ 18pt 或 ≥ 14pt 粗体）        ≥ 3.0 : 1
 * 界面组件与图形对象                    ≥ 3.0 : 1
 * ```
 *
 * DISM 的界面里绝大多数是正文尺寸的文字，所以主要用 4.5。
 */
internal object DismContrast {

    /** 正文文字的最低要求。 */
    const val AA_NORMAL_TEXT = 4.5

    /** 大字与界面组件的最低要求。 */
    const val AA_LARGE_TEXT_OR_COMPONENT = 3.0

    /**
     * 相对亮度（WCAG 定义）。
     *
     * 注意它是**线性的**，而 sRGB 的三个通道要先做
     * 去 gamma 变换——直接用 `0.2126R + 0.7152G + 0.0722B`
     * 会得到错误的结论，而且往往偏乐观。
     *
     * @param argb 0xAARRGGBB，alpha 被忽略
     */
    fun relativeLuminance(
        argb: Long,
    ): Double {
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)

        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * 两个颜色的对比度，1.0 .. 21.0。
     *
     * 顺序无关：WCAG 规定用较亮的作分子。
     */
    fun ratio(
        first: Long,
        second: Long,
    ): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)

        val lighter = maxOf(a, b)
        val darker = minOf(a, b)

        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * sRGB 通道去 gamma。
     *
     * `value` 是 0..255 的原始通道值。
     *
     * ### 为什么不是简单的 `c^2.4`
     *
     * WCAG 给的变换是：
     *
     * ```text
     * c <= 0.03928   →   c / 12.92
     * 否则            →   ((c + 0.055) / 1.055) ^ 2.4
     * ```
     *
     * 那个 `+ 0.055` / `/ 1.055` **不能省**。
     * 省掉之后中灰对白会算成 4.35 而不是 3.95——
     * 也就是说所有对比度都偏乐观，门禁会放过本该失败的配色。
     *
     * 这正是 `theRatioFunctionFollowsWcag` 存在的理由：
     * 一个算错的对比度函数会让所有配色断言"全部通过"。
     */
    private fun channel(
        value: Long,
    ): Double {
        val c = value / 255.0

        return if (c <= 0.03928) {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).pow(2.4)
        }
    }
}
