package me.moiclathy.dism.schedule.api

import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import kotlin.time.Instant

/**
 * Stable product-facing entry point for Dynamic Schedule.
 *
 * 15.3 deliberately exposes capabilities instead of the old
 * DynamicScheduleSession. Persistence, replay storage, provenance,
 * repair and engine assembly remain implementation details.
 */
@DismPublicApi
interface DynamicScheduleFacade {

    /**
     * Read-only historical decision surface.
     */
    val history: DynamicScheduleHistoryFacade

    /**
     * Evaluate the current schedule model **and record it**.
     *
     * Configuration, provider binding and advanced runtime policies
     * remain outside this public facade for now.
     *
     * ### 它会写历史
     *
     * 每调用一次，这个模型里的每个 item 都会多一条
     * Snapshot / Trace / ReplayInput。
     *
     * 展示用途请用 [preview]。
     */
    suspend fun evaluate(
        model: DynamicScheduleModel,
        now: Instant,
    ): DynamicScheduleEvaluation

    /**
     * 16.20：**只评估，不记录**。
     *
     * ```kotlin
     * schedule.preview(model, now)   // 纯读：显示当前决策
     * schedule.evaluate(model, now)  // 读 + 写：记录一次决策
     * ```
     *
     * ### 为什么必须区分这两件事
     *
     * 在加它之前，UI 想显示"当前决策"只能调 [evaluate]——
     * 于是每次打开界面都会给每个 item 写一条新历史：
     *
     * ```text
     * 打开 1 次    → 每个 item 1 条
     * 打开 100 次  → 每个 item 100 条重复记录
     * ```
     *
     * "算一次"与"记一次"是两个独立的意图。
     * 把两者绑在同一个入口上，调用方就没有选择。
     *
     * ### 语义完全一致
     *
     * 它跑的是同一条 Runtime 链路（Dependency → Context →
     * Attention Memory → Adaptation → Projection → Conflict →
     * Explanation），所以同一份输入下
     * `preview(...)` 与 `evaluate(...)` 的 `decisions` 相同——
     * 唯一的差别是有没有落盘。
     */
    suspend fun preview(
        model: DynamicScheduleModel,
        now: Instant,
    ): DynamicScheduleEvaluation

    /**
     * Read the latest persisted decision for one item.
     *
     * This returns a small product view rather than the internal
     * Snapshot / Trace representation.
     */
    suspend fun latestDecision(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionView?

    /**
     * 16.32：一批条目的"最近一次决策"，**一次往返**。
     *
     * ### 为什么需要它
     *
     * 展示层每次加载都要回答一个问题：
     *
     * ```text
     * 哪些条目的决策与已记录的不一样？
     * ```
     *
     * 只有 [latestDecision] 时这必然是 N+1。实测
     * （真实 SQLDelight，见 `LatestDecisionQueryCostTest`）：
     *
     * ```text
     * 200 次 latestDecision       81 ms
     * 一次 preview（整条决策链）    1 ms
     * ```
     *
     * 也就是说逐条查询比它守护的那条评估链**贵约 80 倍**，
     * 而且**每次启动**都会跑。一个日历上百条是很正常的规模。
     *
     * ### 契约
     *
     * 返回的 map **只包含有过记录的条目**。
     * 不在 map 里的 itemId 表示"从未记录过"——那是调用方
     * 需要区别对待的情况（应当记录一次）。
     *
     * 每个条目对应的值与 [latestDecision] 返回的完全一致。
     */
    suspend fun latestDecisions(
        itemIds: Collection<DynamicScheduleItemId>,
    ): Map<
        DynamicScheduleItemId,
        DynamicScheduleDecisionView,
        >
}

/**
 * Product-level result of one scheduling evaluation.
 *
 * It intentionally contains only decision outputs.
 * Internal Runtime, Snapshot, Trace, ReplayInput and persistence
 * records do not cross this boundary.
 */
@DismPublicApi
data class DynamicScheduleEvaluation(
    val evaluatedAt: Instant,
    val decisions: List<DynamicScheduleDecisionView>,
    val diagnostics: List<String> = emptyList(),
) {
    val attentionRequiredCount: Int
        get() = decisions.count {
            it.attentionRequired == true
        }
}

/**
 * Small stable view of one item decision.
 */
@DismPublicApi
data class DynamicScheduleDecisionView(
    val itemId: DynamicScheduleItemId,
    val title: String,
    val priority: Double,

    /**
     * null means that the persisted latest Snapshot does not
     * expose a freshly evaluated attention decision.
     */
    val attentionRequired: Boolean?,

    val explanation: String? = null,
)

/**
 * Read-only product history surface.
 *
 * The public query is deliberately smaller than the internal
 * historical query. Storage/replay flags remain an implementation
 * concern for now.
 */
@DismPublicApi
interface DynamicScheduleHistoryFacade {

    suspend fun query(
        itemId: DynamicScheduleItemId,
        query: DynamicScheduleHistoryQuery =
            DynamicScheduleHistoryQuery(),
    ): DynamicScheduleHistoryResult

    suspend fun latest(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryEntry?
}

/**
 * Product-level history query.
 *
 * Replay / provenance / trace-stage filters are intentionally not
 * part of this public surface yet.
 */
@DismPublicApi
data class DynamicScheduleHistoryQuery(
    val limit: Int = 20,
) {
    init {
        require(limit > 0) {
            "limit must be > 0"
        }
    }
}

/**
 * Product-level historical result.
 */
@DismPublicApi
data class DynamicScheduleHistoryResult(
    val entries: List<DynamicScheduleHistoryEntry>,
    val truncated: Boolean = false,
    val totalMatched: Int = entries.size,
) {
    val isEmpty: Boolean
        get() = entries.isEmpty()

    val latest: DynamicScheduleHistoryEntry?
        get() = entries.firstOrNull()
}

/**
 * Small immutable history entry exposed to applications.
 *
 * Internal Snapshot / Trace / Replay / Provenance objects are
 * deliberately hidden.
 */
@DismPublicApi
data class DynamicScheduleHistoryEntry(
    val itemId: DynamicScheduleItemId,
    val capturedAt: Instant,
    val scheduleRevision: Long,
    val engineVersion: String,
    val hasTrace: Boolean,
)
