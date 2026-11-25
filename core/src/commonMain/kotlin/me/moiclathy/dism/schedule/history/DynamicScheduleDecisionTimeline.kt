package me.moiclathy.dism.schedule.history

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.14.5
 *
 * 某个 item 的决策时间线。
 *
 * ```text
 * entries[0]    = 最新 Decision
 * entries[last] = 最早 Decision
 * ```
 *
 * 与 [DynamicScheduleDecisionHistoryResult.entries] 的顺序一致。
 *
 * ### 关于 `at` / `before` / `after` 的顺序（对设计稿的修正）
 *
 * 设计稿的期望值本身是矛盾的：
 *
 * ```text
 * entries 降序 = [C(10:00), B(09:00), A(08:00)]
 *
 * before(10:00) 期望 [B, A]   ← 降序
 * after(08:00)  期望 [B, C]   ← 升序
 * ```
 *
 * 同一份数据不可能同时满足两者。而且设计稿的实现
 * `entries.lastOrNull { capturedAt <= time }`
 * 在降序下表上会返回**最早**的那条，
 * 但它的测试期望 `at(09:30) == B`（最近的那条）——
 * 实现和期望也对不上。
 *
 * 这里统一成**与 [entries] 相同的"最新在前"顺序**：
 *
 * - [at] 取 `capturedAt <= time` 中**最近**的一条
 *   （降序下就是第一条命中的）；
 * - [before] / [after] 保留时间线顺序。
 *
 * 理由：`latest` / `earliest` / `Result.entries` 全是这个顺序，
 * 让"按时间切片"的访问器换个方向只会制造踩坑的机会。
 *
 * ### 它不是重放
 *
 * [at] 是**历史查表**：
 *
 * > "在 09:00 这个时间点，最近一次已经发生的 Decision 是 B。"
 *
 * 而不是"用 09:00 的状态重算一次"。
 */
data class DynamicScheduleDecisionTimeline(
    val itemId:
        DynamicScheduleItemId,

    val entries:
        List<DynamicScheduleDecisionHistoryEntry>,
) {

    val isEmpty: Boolean
        get() = entries.isEmpty()

    val size: Int
        get() = entries.size

    val latest: DynamicScheduleDecisionHistoryEntry?
        get() = entries.firstOrNull()

    val earliest: DynamicScheduleDecisionHistoryEntry?
        get() = entries.lastOrNull()

    /**
     * 某个时刻"当时最近一次已经发生"的 Decision。
     *
     * 历史查表，不是重放。
     */
    fun at(
        time: Instant,
    ): DynamicScheduleDecisionHistoryEntry? =
        entries.firstOrNull {
            it.capturedAt <= time
        }

    /**
     * 严格早于 [time] 的 Decision（时间线顺序，最新在前）。
     */
    fun before(
        time: Instant,
    ): List<DynamicScheduleDecisionHistoryEntry> =
        entries.filter {
            it.capturedAt < time
        }

    /**
     * 严格晚于 [time] 的 Decision（时间线顺序，最新在前）。
     */
    fun after(
        time: Instant,
    ): List<DynamicScheduleDecisionHistoryEntry> =
        entries.filter {
            it.capturedAt > time
        }

    /**
     * 时间线区间：`[from, until]` 闭区间，时间线顺序。
     */
    fun between(
        from: Instant,
        until: Instant,
    ): List<DynamicScheduleDecisionHistoryEntry> =
        entries.filter {
            it.capturedAt >= from &&
                it.capturedAt <= until
        }

    companion object {

        fun from(
            itemId: DynamicScheduleItemId,

            entries:
                List<DynamicScheduleDecisionHistoryEntry>,
        ): DynamicScheduleDecisionTimeline =
            DynamicScheduleDecisionTimeline(
                itemId = itemId,

                entries =
                    entries.sortedWith(
                        compareByDescending<
                            DynamicScheduleDecisionHistoryEntry,
                            > {
                            it.capturedAt
                        }.thenByDescending {
                            it.snapshotId.value.toString()
                        },
                    ),
            )
    }
}
