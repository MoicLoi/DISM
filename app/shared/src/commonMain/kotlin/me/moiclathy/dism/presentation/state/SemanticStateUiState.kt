package me.moiclathy.dism.presentation.state

/**
 * 16.7：Semantic State 页的状态。
 *
 * 桌面端用来回答一个问题：
 *
 * ```text
 * DISM 现在认为世界是什么样的？
 * ```
 *
 * ### 为什么它只在桌面 / Web 出现
 *
 * 这一页是"钻取型"的：条目多、密度高、需要横向扫。
 * 参考文档明确说这些**不需要在手机上全部显示**。
 */
data class SemanticStateUiState(
    /**
     * 各类计数。
     *
     * 单独抽出来是因为用户第一眼要的就是这个量级感：
     * "系统里现在有多少条状态、多少份证据"。
     */
    val entityCount: Int = 0,

    val evidenceCount: Int = 0,

    val relationCount: Int = 0,

    val transitionCount: Int = 0,

    val relations: List<RelationUi> = emptyList(),

    val isLoading: Boolean = false,

    /**
     * 16.31：这份状态是否**已经读过**。
     *
     * ### 为什么不能只看计数
     *
     * 这一页的代价是"读四张全表"，与库的大小成正比。
     * 所以它**不再**跟着 `DismPresenter.load()` 一起加载，
     * 而是等用户真的打开这一页时才读。
     *
     * 于是出现三种状态：
     *
     * ```text
     * isLoaded = false         还没读过          →  "正在读取…"
     * isLoaded = true，0 条     读过了，确实是空的 →  "DISM 还什么都不知道"
     * isLoaded = true，n 条     读过了，有内容
     * ```
     *
     * 只看计数的话，前两种无法区分——而把"还没读"
     * 显示成"DISM 什么都还不知道"是在撒谎。
     */
    val isLoaded: Boolean = false,

    val errorMessage: String? = null,
) {

    val isEmpty: Boolean
        get() = relationCount == 0 && entityCount == 0

    /**
     * 当前仍成立的关系数。
     *
     * 与 [relationCount] 不同：后者包含已失效的历史状态。
     * 两个数字放在一起才能看出"DISM 记得多少"与
     * "DISM 现在相信多少"的差别。
     */
    val activeRelationCount: Int
        get() = relations.count { it.isActive }
}

/**
 * 一条关系状态的展示形态。
 *
 * `subjectLabel` / `objectLabel` 是**算好的字符串**：
 *
 * ```text
 * Uuid  →  可读标签
 * ```
 *
 * 现在的实现直接给 Uuid（语义层的 Entity 名字还没接进来），
 * 但契约已经把它定位成"标签"而不是"id"——
 * 以后接上 Entity 名字时，UI 不用改。
 */
data class RelationUi(
    val subjectLabel: String,

    val predicateLabel: String,

    val objectLabel: String,

    /**
     * 0.0..1.0。
     */
    val confidence: Double,

    val statusLabel: String,

    val isActive: Boolean,

    /**
     * 有多少条证据支撑它。
     */
    val supportCount: Int,
)
