package me.moiclathy.dism.context

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * DISM 当前进行语义判断时所处的上下文。
 *
 * 目前只保留最核心的信息：
 * - 当前时间
 * - 当前事件
 * - 即将发生的事件
 */
data class Context(
    val now: Instant,
    val activeEventIds: Set<Uuid> = emptySet(),
    val upcomingEvents: List<ContextEvent> = emptyList(),
)