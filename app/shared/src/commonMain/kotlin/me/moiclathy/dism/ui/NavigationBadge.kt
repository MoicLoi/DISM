package me.moiclathy.dism.ui

import me.moiclathy.dism.presentation.DismUiState
import me.moiclathy.dism.presentation.navigation.DismDestination

/**
 * 16.28：导航项上的提示。
 *
 * ### 为什么不放全屏横幅
 *
 * "记录失败"是一个**附属功能**的故障：
 *
 * ```text
 * 日程照常可用        它不紧急
 * 但历史在悄悄缺失    它重要且静默
 * ```
 *
 * 全屏横幅一定会被看到，代价是长期占用主界面的注意力。
 * 而角标：
 *
 * ```text
 * 任何页面都能看到    不占内容空间
 * 且直接指向详情所在页
 * ```
 *
 * 这与"强调必须稀缺"是同一条原则——`Attention` 是全页唯一
 * 使用高饱和容器的区域，因为到处都是强调等于没有强调。
 *
 * ### 它是纯函数，所以能测
 *
 * "哪些目的地该出现角标"是一条产品规则，不该埋在
 * `when` 表达式里靠肉眼维护。
 */
internal fun badgeFor(
    destination: DismDestination,
    state: DismUiState,
): String? =
    when (destination) {
        /*
         * 记录失败 → 历史不完整 → 角标落在 History 上。
         *
         * 用文字而不是圆点：圆点需要用户知道
         * "那个点是什么意思"，而且它在明暗主题下表现不同。
         * 文字没有这个歧义，也更容易被无障碍工具读出来。
         */
        DismDestination.HISTORY ->
            if (state.history.historyMayBeIncomplete) {
                "!"
            } else {
                null
            }

        /*
         * 其它目的地目前没有需要提示的状态。
         *
         * 这里是穷举而不是 `else`：将来某个页面有了新状态，
         * 编译器会提醒我们在这里做一次决定，
         * 而不是让它悄悄没有角标。
         */
        DismDestination.TODAY,
        DismDestination.SCHEDULE,
        DismDestination.ATTENTION,
        DismDestination.MEMORY,
        DismDestination.SETTINGS,
        DismDestination.SEMANTIC_STATE,
        DismDestination.DIAGNOSTICS,
        ->
            null
    }

/**
 * 角标要传达给无障碍工具的完整语义。
 *
 * 只有 `"!"` 是不够的：读屏会念出"History 感叹号"，
 * 用户并不知道发生了什么。
 */
internal fun badgeDescriptionFor(
    destination: DismDestination,
    state: DismUiState,
): String? =
    when (destination) {
        DismDestination.HISTORY ->
            if (state.history.historyMayBeIncomplete) {
                "History may be incomplete: " +
                    "recent decisions could not be recorded."
            } else {
                null
            }

        else -> null
    }
