package me.moiclathy.dism.presentation.navigation

/**
 * 16.3：DISM 的顶层目的地。
 *
 * ### 为什么手机与桌面共用同一份枚举
 *
 * 参考文档要求"多端不要做成同一个 UI 缩放"，但**目的地是同一套**：
 *
 * ```text
 * 手机      Bottom Navigation: Today / Schedule / Attention / Memory / Settings
 * 桌面      左侧导轨：同上 + State / History / Diagnostics
 * Web       同上
 * ```
 *
 * 差别在"一次显示几个""一屏能放多少信息""能钻多深"，
 * 不在"有哪些地方可去"。
 *
 * 所以 [primary] 与 [desktopOnly] 描述的是**导航位置**，
 * 不是页面本身。
 */
enum class DismDestination(
    /**
     * 是否进入手机端底部导航。
     *
     * 手机端刻意只放少数几个：单手可及，且信息密度低。
     */
    val primary: Boolean,

    /**
     * 是否只在桌面 / Web 端出现。
     *
     * 这些是"钻取型"视图：诊断、关系、上下文细节。
     * 手机上不需要全部显示。
     */
    val desktopOnly: Boolean = false,

    /**
     * 16.33：**页面是否已经实现**。
     *
     * ### 为什么需要这个标志
     *
     * 导航只应该列出**能打开的**目的地：
     *
     * ```text
     * 放一个点进去显示"敬请期待"的入口  →  用户以为功能坏了
     * ```
     *
     * 在此之前这个判断是一句硬编码的
     * `filter { it != DismDestination.DIAGNOSTICS }`，
     * 而 **MEMORY 漏掉了**——它 `primary = true`，
     * 所以一直出现在导航里，点进去是一条"未实现"的消息。
     *
     * 更糟的是 [me.moiclathy.dism.ui.DismApp] 的注释里写着
     * "Memory 不在导航里"：**文档与行为不一致，而且没人发现**。
     *
     * 把"有没有页面"变成字段之后：
     *
     * ```text
     * 导航 = filter { it.implemented }
     * ```
     *
     * 规则只有一处，而且新增目的地时**必须**显式声明
     * 它是否已实现——不能靠忘记而蒙混过去。
     */
    val implemented: Boolean = true,
) {

    TODAY(primary = true),

    SCHEDULE(primary = true),

    /**
     * 16.6：为什么系统提醒我。
     *
     * 它值得占据一个主目的地，而不是藏在 Today 里：
     * 用户点进来通常正因为"它凭什么提醒我"。
     */
    ATTENTION(primary = true),

    /**
     * 记忆页。
     *
     * `implemented = false`：页面还没做，所以**不出现**在导航里。
     * 等页面做出来时把这一位改成 true，导航会自动带上它。
     */
    MEMORY(primary = true, implemented = false),

    SETTINGS(primary = true),

    /**
     * 语义状态总览。
     *
     * 桌面端用来回答"DISM 现在认为世界是什么样的"。
     */
    SEMANTIC_STATE(primary = false, desktopOnly = true),

    /**
     * 历史决策与重放。
     */
    HISTORY(primary = false, desktopOnly = true),

    /**
     * 诊断与 Provider 状态。
     *
     * `implemented = false`：页面还没做。
     */
    DIAGNOSTICS(
        primary = false,
        desktopOnly = true,
        implemented = false,
    ),
}
