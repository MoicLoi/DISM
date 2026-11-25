package me.moiclathy.dism.ui

import me.moiclathy.dism.presentation.navigation.DismDestination

/**
 * 16.33：**哪些目的地在哪种窗口尺寸下出现**。
 *
 * ### 为什么它值得是一个可测的纯函数
 *
 * 这是一条产品规则，不是布局细节：
 *
 * ```text
 * 手机     底部导航，只放 primary
 * 平板/桌面 导轨，加上钻取型页面
 * ```
 *
 * 埋在 Composable 的 `filter` 里时，它只有靠 UI 测试
 * 才能覆盖——而那需要一整套新基建。抽出来之后，
 * 每种尺寸的导航项集合都能直接断言。
 *
 * ### 规则
 *
 * ```text
 * implemented = false 的目的地   任何尺寸下都不出现
 * primary 目的地                 始终出现
 * desktopOnly 目的地             COMPACT 下不出现
 * ```
 *
 * 第一条是 16.33 加的：在此之前这个判断是一句硬编码的
 * `filter { it != DIAGNOSTICS }`，而 MEMORY 漏掉了——
 * 它 `primary = true`，于是**一直出现在导航里**，
 * 点进去是一条"未实现"的消息。
 *
 * 现在规则只有一处，而且新增目的地时**必须**显式声明
 * 它是否已实现。
 */
internal fun navDestinationsFor(
    size: DismWindowSize,
): List<DismDestination> =
    DismDestination.entries
        .filter { destination ->
            /*
             * 没有页面的目的地一律不列。
             *
             * 一个点进去说"敬请期待"的入口比没有入口更糟：
             * 用户会以为功能坏了。
             */
            destination.implemented
        }
        .filter { destination ->
            destination.primary ||
                (size != DismWindowSize.COMPACT &&
                    destination.desktopOnly)
        }

/**
 * 16.33：内容区的最大宽度。
 *
 * ### 为什么宽屏上要限制内容宽度
 *
 * 不限制的话，桌面全屏时一行文字会横跨 2000px 以上：
 *
 * ```text
 * "09:00  实验动物学  ·  实验楼201  ·  Upcoming"
 *         ←────── 视线要走很远才到行尾 ──────→
 * ```
 *
 * 行太长会让人在换行时**丢行**（读到下一行开头找不到位置）。
 * 舒适的正文行宽大约在 45–75 个字符，对应 600–840dp。
 *
 * 所以内容区封顶在 [EXPANDED_MIN_WIDTH_DP]，超出的部分
 * 留白居中。这与"窗口宽就多放内容"是两回事：
 *
 * ```text
 * 该变宽的是       导航形态、能并列显示几栏
 * 不该变宽的是     单栏文字的宽度
 * ```
 *
 * 这个上限刻意等于 EXPANDED 的断点，而不是另取一个数字——
 * "什么时候算宽"与"内容最多多宽"用同一个数量级更容易理解。
 */
internal val MAX_CONTENT_WIDTH_DP =
    DismBreakpoints.EXPANDED_MIN_WIDTH_DP
