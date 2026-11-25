package me.moiclathy.dism.presentation.state

import me.moiclathy.dism.api.Dism

/**
 * 16.31：库级状态的加载器（**按需**）。
 *
 * ### 为什么它从 `DismPresenter` 里独立出来
 *
 * 在这之前，`DismPresenter.load()` 每次都会调
 * `dism.runtime.snapshot()`，而那个调用：
 *
 * ```text
 * 读 4 张全表（entities / evidence / relationStates / transitions）
 * 返回全部数据
 * ```
 *
 * 代价只取决于**库有多大**，不取决于用户在看什么。
 *
 * 而它的产物只用于两件事：
 *
 * ```text
 * 四个计数                     一个页面顶部的一行
 * 关系列表                     Semantic State 页
 * ```
 *
 * 那一页是 `desktopOnly` 的——**手机端根本打不开**，
 * 却照样为它读四张全表。
 *
 * ### 现在的形状
 *
 * ```text
 * DismPresenter.load()        日程相关，与条目数成正比
 * SemanticStatePresenter.load()  库级，与库的大小成正比
 * ```
 *
 * 后者只在用户真的选中那一页时才调用。
 *
 * ### 它不是"优化得差不多就行"
 *
 * "读全表"与"读一个计数"的区别在库变大时会拉开数量级。
 * 而 `load()` 是在**每次启动**与**每次模型变化**时跑的。
 */
class SemanticStatePresenter(
    private val dism: Dism,
) {

    /**
     * 读取库级状态。
     *
     * 失败**不抛**：这一页是只读的概览，
     * 让它失败掉整页不如显示一条错误。
     */
    fun load(): SemanticStateUiState =
        try {
            semanticStateUiState(
                snapshot = dism.runtime.snapshot(),
            )
        } catch (error: Exception) {
            SemanticStateUiState(
                isLoading = false,

                isLoaded = true,

                errorMessage =
                    error.message
                        ?: "Failed to read the library state.",
            )
        }
}
