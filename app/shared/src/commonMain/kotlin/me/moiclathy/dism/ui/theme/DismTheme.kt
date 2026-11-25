package me.moiclathy.dism.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 16.2：DISM 设计系统。
 *
 * ### 它的职责边界
 *
 * ```text
 * DismTheme      颜色 / 排版 / 间距的**唯一**来源
 * 页面            只使用语义 token，不写死颜色与尺寸
 * ```
 *
 * 一旦某页开始写 `Color(0xFF...)` 或 `16.dp`，
 * 后面做 16.13 响应式布局时就只能去那一页里翻。
 *
 * ### 为什么强调"克制"
 *
 * DISM 的界面要回答"我现在该注意什么"，
 * 所以视觉层级必须由**注意力**驱动，而不是由装饰驱动：
 *
 * ```text
 * 时间轴     安静、低对比
 * Attention  唯一高饱和的区域
 * ```
 *
 * 这一点决定了下面强调色只用在少数位置。
 */
@Composable
fun DismTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DismDarkColors
            } else {
                DismLightColors
            },
        typography = DismTypography,
        content = content,
    )
}

/**
 * 间距标尺。
 *
 * 用对象而不是散落的 `dp` 字面量：跨端（手机 / 桌面 / Web）
 * 调整密度时只需要改这里。
 */
object DismSpacing {

    val xs = 4.dp

    val sm = 8.dp

    val md = 12.dp

    val lg = 16.dp

    val xl = 24.dp

    val xxl = 32.dp

    /**
     * 时间标签列的固定宽度。
     *
     * 固定它，时间轴才会看起来是一条竖线；
     * 让它自适应，"09:00" 与 "11:30" 就会左右抖动。
     */
    val timeColumn = 56.dp
}

/**
 * 16.30：颜色来自 [DismPalette]，不在这里重复写数字。
 *
 * 对比度门禁（`DismContrastTest`）检查的就是那份常量，
 * 所以"主题改了但门禁还在查旧颜色"在结构上不可能发生。
 */
private val DismLightColors =
    lightColorScheme(
        primary = Color(DismPalette.LIGHT_PRIMARY),
        onPrimary = Color(DismPalette.LIGHT_ON_PRIMARY),
        primaryContainer =
            Color(DismPalette.LIGHT_PRIMARY_CONTAINER),
        onPrimaryContainer =
            Color(DismPalette.LIGHT_ON_PRIMARY_CONTAINER),
        secondary = Color(DismPalette.LIGHT_SECONDARY),
        onSecondary = Color(DismPalette.LIGHT_ON_SECONDARY),
        background = Color(DismPalette.LIGHT_BACKGROUND),
        onBackground = Color(DismPalette.LIGHT_ON_BACKGROUND),
        surface = Color(DismPalette.LIGHT_SURFACE),
        onSurface = Color(DismPalette.LIGHT_ON_SURFACE),
        surfaceVariant =
            Color(DismPalette.LIGHT_SURFACE_VARIANT),
        onSurfaceVariant =
            Color(DismPalette.LIGHT_ON_SURFACE_VARIANT),
        error = Color(DismPalette.LIGHT_ERROR),
        onError = Color(DismPalette.LIGHT_ON_ERROR),
    )

private val DismDarkColors =
    darkColorScheme(
        primary = Color(DismPalette.DARK_PRIMARY),
        onPrimary = Color(DismPalette.DARK_ON_PRIMARY),
        primaryContainer =
            Color(DismPalette.DARK_PRIMARY_CONTAINER),
        onPrimaryContainer =
            Color(DismPalette.DARK_ON_PRIMARY_CONTAINER),
        secondary = Color(DismPalette.DARK_SECONDARY),
        onSecondary = Color(DismPalette.DARK_ON_SECONDARY),
        background = Color(DismPalette.DARK_BACKGROUND),
        onBackground = Color(DismPalette.DARK_ON_BACKGROUND),
        surface = Color(DismPalette.DARK_SURFACE),
        onSurface = Color(DismPalette.DARK_ON_SURFACE),
        surfaceVariant =
            Color(DismPalette.DARK_SURFACE_VARIANT),
        onSurfaceVariant =
            Color(DismPalette.DARK_ON_SURFACE_VARIANT),
        error = Color(DismPalette.DARK_ERROR),
        onError = Color(DismPalette.DARK_ON_ERROR),
    )

/**
 * 默认 Material3 排版，但把标题收紧一档。
 *
 * Today 页的标题（"Today"）不应该比页面里的日程标题更抢眼——
 * 用户是来看日程的，不是来看页名的。
 */
private val DismTypography =
    Typography().let { base ->
        base.copy(
            headlineMedium =
                base.headlineMedium.copy(
                    letterSpacing =
                        base.headlineMedium.letterSpacing * 0.5,
                ),
            titleLarge =
                base.titleLarge.copy(
                    letterSpacing =
                        base.titleLarge.letterSpacing * 0.5,
                ),
        )
    }
