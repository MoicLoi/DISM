package me.moiclathy.dism.presentation.attention

/**
 * 16.6：Attention 页一次渲染所需的状态。
 *
 * ### 它与 Today 里的 Attention 区不同
 *
 * ```text
 * Today 的 Attention 区    当前最该注意的几条（通常 3~5）
 * Attention 页             全部需要关注的原因，以及"为什么"
 * ```
 *
 * Today 要做减法，Attention 页做的是**解释**：
 * 用户点进来通常是因为"它凭什么提醒我"。
 */
data class AttentionUiState(
    val entries: List<AttentionEntryDetailUi> = emptyList(),

    val isLoading: Boolean = false,

    val errorMessage: String? = null,
) {

    val isEmpty: Boolean
        get() = entries.isEmpty()

    val size: Int
        get() = entries.size

    /**
     * 被判定为需要关注、但没给出解释的条数。
     *
     * 这个数字暴露出来是有意的：
     *
     * ```text
     * 0  →  每条提醒都能回答"为什么"
     * >0 →  有提醒无法解释，属于需要修的可观测问题
     * ```
     */
    val unexplainedCount: Int
        get() = entries.count { it.reason == null }
}

/**
 * Attention 页的一条。
 *
 * `reason` 可以为 null——那表示**决策说要关注，但没给出解释**。
 * 这比编一句"需要你的注意"要诚实：后者会让
 * [AttentionUiState.unexplainedCount] 永远为 0，
 * 于是这个可观测指标就失效了。
 */
data class AttentionEntryDetailUi(
    val itemId: String,

    val title: String,

    val timeLabel: String?,

    /**
     * 0.0..1.0。
     */
    val priority: Double,

    val reason: String?,
)
