package me.moiclathy.dism.schedule.history

import kotlin.time.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage

/**
 * 12.x / 13.13
 *
 * 统一历史决策查询。
 *
 * ### 默认绝不重放（13.13.2）
 *
 * [includeReplay] 默认 `false`。
 *
 * 否则：
 *
 * ```text
 * runtime.decisionHistory(itemId)
 *        ↓
 * 把 100 条历史记录全部 Replay
 *        ↓
 * 再次执行 Runtime 100 次
 * ```
 *
 * "查询历史"和"重新执行历史"是两件事，
 * 不能让一次读取操作悄悄变成昂贵的计算。
 */
data class DynamicScheduleDecisionHistoryQuery(
    val itemId: DynamicScheduleItemId? = null,

    val from: Instant? = null,

    val until: Instant? = null,

    val scheduleRevision: Long? = null,

    val traceStage: DynamicScheduleDecisionTraceStage? = null,

    val decisiveOnly: Boolean = false,

    val limit: Int = 50,

    /**
     * 是否加载 Trace。
     *
     * 注意：使用了 [traceStage] / [decisiveOnly] 时
     * 会**强制**加载 Trace，否则这些过滤条件没有意义。
     */
    val includeTrace: Boolean = true,

    /**
     * 是否也加载持久化的来源链（如果这条历史被重放过）。
     *
     * 这是**读取已有记录**，不重新执行 Runtime，
     * 所以默认开启。
     */
    val includeProvenance: Boolean = true,

    /**
     * 是否执行 Historical Replay。
     *
     * 默认 false：查询历史不应该因为查看历史而重新运行 Runtime。
     */
    val includeReplay: Boolean = false,

    /**
     * 是否在 Replay 之后执行 Verification。
     *
     * 只有 [includeReplay] 为 true 时才有意义。
     */
    val includeVerification: Boolean = false,
) {

    init {
        require(from == null || until == null || until >= from)
        require(scheduleRevision == null || scheduleRevision >= 0)
        require(limit > 0)

        require(
            !includeVerification || includeReplay,
        ) {
            "includeVerification requires includeReplay=true."
        }
    }

    /**
     * 实际是否需要读取 Trace 内容。
     */
    val needsTrace: Boolean
        get() =
            includeTrace ||
                traceStage != null ||
                decisiveOnly
}
