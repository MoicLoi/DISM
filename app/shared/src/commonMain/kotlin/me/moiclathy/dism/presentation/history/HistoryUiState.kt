package me.moiclathy.dism.presentation.history

/**
 * 16.8：History 页的状态。
 *
 * ### 它是**按 item 查询**的，不是全局流水
 *
 * 公共 Facade 的形状就是：
 *
 * ```kotlin
 * history.query(itemId, DynamicScheduleHistoryQuery(limit))
 * history.latest(itemId)
 * ```
 *
 * 没有"查全部历史"的方法。这不是遗漏，而是有意的：
 * 一次查全部历史是一个会随历史增长而无上限变慢的操作
 * （15.16 的 `DynamicScheduleDecisionProvenanceQuery` 也是
 * 出于同样理由强制要求 itemId 或 snapshotId）。
 *
 * 所以这一页的结构是"选一条 → 看它的历史"，而不是一张大表。
 */
data class HistoryUiState(
    /**
     * 可以查历史的条目（来自当前模型）。
     */
    val items: List<HistoryItemUi> = emptyList(),

    /**
     * 当前选中的 item。
     */
    val selectedItemId: String? = null,

    /**
     * 选中条目的历史，最近一次在前。
     */
    val entries: List<HistoryEntryUi> = emptyList(),

    /**
     * 查询命中的总条数。
     *
     * 与 `entries.size` 可以不同：后者可能被 limit 截断。
     * 把两者都暴露出来，UI 才能诚实地说
     * "显示最近 20 条，共 37 条"。
     */
    val totalMatched: Int = 0,

    val truncated: Boolean = false,

    val isLoading: Boolean = false,

    val errorMessage: String? = null,

    /**
     * 16.26：最近一次"记录决策"失败的原因。
     *
     * ### 为什么它必须出现在界面上
     *
     * `DecisionRecorder` 有意吞掉记录失败（宁可历史少一条，
     * 也不能让 Today 变成错误页），并把原因发到诊断。
     *
     * 但诊断是**开发者**的通道。只有它的话，用户看到的是：
     *
     * ```text
     * 一个空的 History 页，没有任何解释
     * ```
     *
     * "页面看起来正常、内容却是错的"是最难被发现的一类问题——
     * 用户会以为是 DISM 没记录，而不是记录失败了。
     *
     * ### 它为什么会出现又消失
     *
     * 判定"是否要记录"是拿当前决策与**已存下来的**比对。
     * 所以一次失败的写入会让下次加载仍然判定"变了"、于是重试；
     * 成功之后这个字段自动变回 null。
     *
     * 也就是说它反映的是"**现在**记录功能是否可用"，
     * 而不是一个历史累计错误。
     */
    val recordingError: String? = null,
) {

    val isEmpty: Boolean
        get() = entries.isEmpty()

    val hasSelection: Boolean
        get() = selectedItemId != null

    val latestEntry: HistoryEntryUi?
        get() = entries.firstOrNull()

    /**
     * 记录功能当前是否正常。
     */
    val canRecord: Boolean
        get() = recordingError == null

    /**
     * 界面是否该提示"历史可能不完整"。
     */
    val historyMayBeIncomplete: Boolean
        get() = recordingError != null
}

/**
 * 可查询历史的一条日程。
 */
data class HistoryItemUi(
    val itemId: String,

    val title: String,
)

/**
 * 一次历史决策。
 *
 * `hasTrace` 单独暴露，因为它是**可信度**信息：
 *
 * ```text
 * 有 Trace  →  这次决策的执行过程是可查的
 * 没有 Trace →  只剩快照，无法解释当时为什么这么算
 * ```
 */
data class HistoryEntryUi(
    val itemId: String,

    val capturedAtLabel: String,

    val scheduleRevision: Long,

    val engineVersion: String,

    val hasTrace: Boolean,
)
