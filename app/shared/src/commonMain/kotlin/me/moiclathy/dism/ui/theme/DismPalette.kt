package me.moiclathy.dism.ui.theme

/**
 * 16.30：配色的**唯一来源**。
 *
 * ### 为什么颜色要以 ARGB 常量单独存一份
 *
 * 对比度门禁（`DismContrastTest`）需要拿到这些数字。
 * 如果主题里写 `Color(0xFF3B5BDB)`、测试里再写一遍，
 * 就会变成两份会漂移的常量：
 *
 * ```text
 * 有人调了主题里的 primary
 * → 测试仍在检查**旧**颜色
 * → 门禁继续通过，而实际配色已经不合格
 * ```
 *
 * 所以颜色在这里定义一次，主题与测试都引用它。
 * 漂移在结构上不可能发生。
 *
 * ### 为什么是 `Long` 而不是 `Color`
 *
 * `Color` 的比较涉及色彩空间（`Color(0xFF3B5BDB)` 会落到
 * sRGB 空间，而 `==` 会连同空间一起比），把它当数据断言不直观。
 * ARGB 整数是这些颜色本来的样子，也是对比度公式的输入。
 *
 * `DismTheme.kt` 负责把它们变成 `Color`。
 *
 * ### 命名
 *
 * 用 Material3 的**角色名**（`onSurfaceVariant`）而不是
 * 描述性名字（`mutedText`）：
 *
 * ```text
 * 角色名   →  换配色时不用改名字，引用方也不用改
 * 描述名   →  "primary" 被改成绿色之后，名字就开始撒谎
 * ```
 */
internal object DismPalette {

    // ── 浅色 ──────────────────────────────────────────

    const val LIGHT_PRIMARY = 0xFF3B5BDB
    const val LIGHT_ON_PRIMARY = 0xFFFFFFFF
    const val LIGHT_PRIMARY_CONTAINER = 0xFFDDE3FF
    const val LIGHT_ON_PRIMARY_CONTAINER = 0xFF00174B

    const val LIGHT_SECONDARY = 0xFF5A5D72
    const val LIGHT_ON_SECONDARY = 0xFFFFFFFF

    const val LIGHT_BACKGROUND = 0xFFFAF9FC
    const val LIGHT_ON_BACKGROUND = 0xFF1A1B21

    const val LIGHT_SURFACE = 0xFFFAF9FC
    const val LIGHT_ON_SURFACE = 0xFF1A1B21

    const val LIGHT_SURFACE_VARIANT = 0xFFE2E1EC
    const val LIGHT_ON_SURFACE_VARIANT = 0xFF45464F

    const val LIGHT_ERROR = 0xFFBA1A1A
    const val LIGHT_ON_ERROR = 0xFFFFFFFF

    // ── 深色 ──────────────────────────────────────────

    const val DARK_PRIMARY = 0xFFB6C4FF
    const val DARK_ON_PRIMARY = 0xFF082978
    const val DARK_PRIMARY_CONTAINER = 0xFF22409A
    const val DARK_ON_PRIMARY_CONTAINER = 0xFFDDE3FF

    const val DARK_SECONDARY = 0xFFC3C5DD
    const val DARK_ON_SECONDARY = 0xFF2C2F42

    const val DARK_BACKGROUND = 0xFF121318
    const val DARK_ON_BACKGROUND = 0xFFE3E1E9

    const val DARK_SURFACE = 0xFF121318
    const val DARK_ON_SURFACE = 0xFFE3E1E9

    const val DARK_SURFACE_VARIANT = 0xFF45464F
    const val DARK_ON_SURFACE_VARIANT = 0xFFC6C5D0

    const val DARK_ERROR = 0xFFFFB4AB
    const val DARK_ON_ERROR = 0xFF690005
}
