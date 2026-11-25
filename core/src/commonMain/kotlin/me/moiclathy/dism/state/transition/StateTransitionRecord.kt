package me.moiclathy.dism.state.transition

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 一条已被成功应用的状态变化的语义记录。
 *
 * 与 TimelineChangeSet 分工不同：
 * - StateTransitionRecord = 是什么语义操作导致了变化（intent / semantic cause）
 * - TimelineChangeSet = 数据结构具体改了什么（materialized state diff）
 *
 * 一次 retroactive reconciliation 可以产生 1 条 TransitionRecord
 * 和 N 条 StateChange（shorten / insert / merge）。
 */
data class StateTransitionRecord(
    val id: Uuid = Uuid.random(),
    val effectiveAt: Instant,
    val transition: StateTransition,
)
